from os.path import join, dirname
import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from torch.utils.data import DataLoader, Dataset
import numpy as np
import cv2
#from sklearn.model_selection import train_test_split

# Paths
LFW_PATH = './lfw'


# Helper function to load image
def load_image(filepath, target_size=(100, 100)):
    img = cv2.imread(filepath)
    img = cv2.resize(img, target_size)
    img = img.astype('float32') / 255.0  # Normalize
    img = np.transpose(img, (2, 0, 1))  # Convert to (channels, height, width)
    return img


# Dataset class for loading image pairs
class LFWDataset(Dataset):
    def __init__(self, X1, X2, y):
        self.X1 = X1
        self.X2 = X2
        self.y = y

    def __len__(self):
        return len(self.y)

    def __getitem__(self, idx):
        return torch.tensor(self.X1[idx]), torch.tensor(self.X2[idx]), torch.tensor(self.y[idx], dtype=torch.float32)


# Define the base CNN network (VGG-like)
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
            nn.MaxPool2d(kernel_size=2, stride=2)
        )

        self.fc1 = nn.Linear(256 * 12 * 12, 512)
        self.fc2 = nn.Linear(512, 128)

    def forward(self, x):
        x = self.features(x)
        x = x.view(x.size(0), -1)
        x = F.relu(self.fc1(x))
        x = self.fc2(x)
        return x


# Siamese network definition
class SiameseNetwork(nn.Module):
    def __init__(self):
        super(SiameseNetwork, self).__init__()
        self.base_network = BaseNetwork()

    def forward(self, input1, input2):
        output1 = self.base_network(input1)
        output2 = self.base_network(input2)
        return output1, output2


# Contrastive loss function
class ContrastiveLoss(nn.Module):
    def __init__(self, margin=1.0):
        super(ContrastiveLoss, self).__init__()
        self.margin = margin

    def forward(self, output1, output2, label):
        euclidean_distance = F.pairwise_distance(output1, output2)
        loss_contrastive = torch.mean(
            label * torch.pow(euclidean_distance, 2) +
            (1 - label) * torch.pow(torch.clamp(self.margin - euclidean_distance, min=0.0), 2)
        )
        return loss_contrastive

def isSamePerson(ar, img2_path):
    model = SiameseNetwork()
    model.load_state_dict(torch.load(join(dirname(__file__), "weights/siamese_model.pth")))
    model.eval()

    img1s = []
    res = []
    for i in ar:
        img1s.append(torch.tensor([load_image(i)], dtype=torch.float32))
    img2 = torch.tensor([load_image(img2_path)], dtype=torch.float32)

    with torch.no_grad():
        for i in img1s:
            embedding_a, embedding_b = model(i, img2)
            distance = F.pairwise_distance(embedding_a, embedding_b)
            res.append(distance.item())

    return res


# Example usage
if __name__ == '__main__':
    ar = ["0.jpg", "1.jpg", "2.jpg", "3.jpg", "4.jpg",
          "5.jpg", "6.jpg", "7.jpg", "8.jpg", "9.jpg", "10.jpg"]
    dum = []
    for i in range(4):
        arrr = isSamePerson(ar, "Tmp.jpg")
        print(arrr)
        dum = arrr
    print(dum.index(min(dum)))