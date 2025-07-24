import os
import random
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
import cv2

LFW_PATH = './lfw'

def load_image(filepath, target_size=(100, 100)):
    img = cv2.imread(filepath)
    img = cv2.resize(img, target_size)
    img = img.astype('float32') / 255.0
    img = np.transpose(img, (2, 0, 1))
    return img


def load_lfw_pairs(lfw_path):
    same_pairs = []
    different_pairs = []
    all_folders = [os.path.join(lfw_path, folder) for folder in os.listdir(lfw_path) if
                   os.path.isdir(os.path.join(lfw_path, folder))]

    for folder in all_folders:
        images = [os.path.join(folder, img) for img in os.listdir(folder) if img.endswith('.jpg')]
        if len(images) > 1:
            same_pairs.append((images[0], images[1], 1))

    while len(different_pairs) < len(same_pairs):
        person1_folder, person2_folder = random.sample(all_folders, 2)
        img1 = random.choice(
            [os.path.join(person1_folder, img) for img in os.listdir(person1_folder) if img.endswith('.jpg')])
        img2 = random.choice(
            [os.path.join(person2_folder, img) for img in os.listdir(person2_folder) if img.endswith('.jpg')])
        different_pairs.append((img1, img2, 0))

    pairs = same_pairs + different_pairs
    random.shuffle(pairs)

    X1 = np.array([load_image(pair[0]) for pair in pairs])
    X2 = np.array([load_image(pair[1]) for pair in pairs])
    y = np.array([pair[2] for pair in pairs])

    return X1, X2, y

class LFWDataset(Dataset):
    def __init__(self, X1, X2, y):
        self.X1 = X1
        self.X2 = X2
        self.y = y

    def __len__(self):
        return len(self.y)

    def __getitem__(self, idx):
        return torch.tensor(self.X1[idx], dtype=torch.float32), \
               torch.tensor(self.X2[idx], dtype=torch.float32), \
               torch.tensor(self.y[idx], dtype=torch.float32)

class BaseNetwork(nn.Module):
    def __init__(self):
        super(BaseNetwork, self).__init__()
        self.features = nn.Sequential(
            nn.Conv2d(3, 64, kernel_size=3, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            nn.Conv2d(128, 256, kernel_size=3, padding=1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            nn.Flatten()
        )
        self.fc1 = nn.Linear(256 * 12 * 12, 512)
        self.fc2 = nn.Linear(512, 128)

    def forward(self, x):
        x = self.features(x)
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return x

class SiameseNetwork(nn.Module):
    def __init__(self):
        super(SiameseNetwork, self).__init__()
        self.base_network = BaseNetwork()

    def forward(self, input1, input2):
        output1 = self.base_network(input1)
        output2 = self.base_network(input2)
        return output1, output2

class ContrastiveLoss(nn.Module):
    def __init__(self, margin=1.0):
        super(ContrastiveLoss, self).__init__()
        self.margin = margin

    def forward(self, output1, output2, label):
        euclidean_distance = F.pairwise_distance(output1, output2)
        loss_contrastive = torch.mean((1 - label) * torch.pow(euclidean_distance, 2) +
                                      (label) * torch.pow(torch.clamp(self.margin - euclidean_distance, min=0.0), 2))
        return loss_contrastive

def train_model(X1, X2, y, batch_size=32, epochs=10, lr=0.0001):
    dataset = LFWDataset(X1, X2, y)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)
    model = SiameseNetwork()
    criterion = ContrastiveLoss()
    optimizer = optim.Adam(model.parameters(), lr=lr)
    model.train()
    for epoch in range(epochs):
        running_loss = 0.0
        for i, data in enumerate(dataloader, 0):
            img1, img2, labels = data
            optimizer.zero_grad()
            output1, output2 = model(img1, img2)
            loss = criterion(output1, output2, labels)
            loss.backward()
            optimizer.step()
            running_loss += loss.item()
            if i % 10 == 9:
                print(f"[{epoch + 1}, {i + 1}] loss: {running_loss / 10:.4f}")
                running_loss = 0.0
    return model


if __name__ == '__main__':
    X1, X2, y = load_lfw_pairs(LFW_PATH)
    model = train_model(X1, X2, y)
    print("Training complete.")
    torch.save(model.state_dict(), 'siamese_model.pth')