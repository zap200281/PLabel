import argparse
import os
import sys

import json
import numpy as np
from PIL import Image
import requests
from skimage import measure
import torch
import torch.nn.functional as F
from torchvision import transforms

from unet import UNet
from utils.dataset import BasicDataset

from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# Configuration
thr = 100
scale = 1.0
mask_threshold = 0.5

class post_messages:
    def __init__(self,urls):
        self.urls = urls
        
    def post(self, datajson):
        #print(datajson)
        response = requests.post(self.urls + '/api/message/', json=datajson,verify=False)
        return response.text


def parse_args():
    """
    Parse input arguments
    """
    parser = argparse.ArgumentParser(description='Train a Fast R-CNN network')

    parser.add_argument('--cfg', dest='cfg',
                        help='config file path',
                        default='configs/pascal_voc/faster_rcnn_r50_fpn_1x_voc0712.py', type=str)
    parser.add_argument('--checkpoint', dest='checkpoint',
                        help='model path',
                        default='./checkpoints/no_normalize/CP_epoch3.pth', type=str)
    parser.add_argument('--image_dir', dest='image_dir',
                        help='directory to load images for demo',
                        default="images")
    parser.add_argument('--output_dir', dest='output_dir',
                        help='directory to load images for demo',
                        default="output/")
    parser.add_argument('--reid', dest='reid',
                        help='is reid detection.',
                        default=False)
    parser.add_argument('--taskid', dest='taskid',
                        help='taskid for send message',
                        default=False)
    parser.add_argument('--msgrest', dest='msgrest',
                        help='http url for send message.',
                        default=False)
    parser.add_argument('--gpu', dest='gpu',
                        help='gpu number for run.',
                        default="0")                           

    args = parser.parse_args()
    return args

image_list = []; 
def DirAll(pathName):
    if os.path.exists(pathName):
        fileList = os.listdir(pathName)
        for f in fileList:
            f=os.path.join(pathName, f)
            if os.path.isdir(f):
                DirAll(f)
            else:
                dirName = os.path.dirname(f)
                baseName = os.path.basename(f)
                if dirName.endswith(os.sep):
                    image_list.append(dirName+baseName)
                else:
                    image_list.append(dirName+os.sep+baseName)


def init_model(model_path, device):
    net = UNet(n_channels=1, n_classes=1)
    print("Loading model {}".format(model_path))

    net.to(device=device)
    net.load_state_dict(torch.load(model_path, map_location=device))
    print("Model loaded !")
    return net


def predict_img(net,
                full_img,
                device,
                scale_factor=1,
                out_threshold=0.5,
                if_norm=False):
    net.eval()

    img = torch.from_numpy(BasicDataset.preprocess(full_img, scale_factor, if_norm=if_norm))

    img = img.unsqueeze(0)
    img = img.to(device=device, dtype=torch.float32)

    with torch.no_grad():
        output = net(img)

        if net.n_classes > 1:
            probs = F.softmax(output, dim=1)
        else:
            probs = torch.sigmoid(output)

        probs = probs.squeeze(0)

        tf = transforms.Compose(
            [
                transforms.ToPILImage(),
                transforms.Resize(full_img.size[1]),
                transforms.ToTensor()
            ]
        )

        probs = tf(probs.cpu())
        full_mask = probs.squeeze().cpu().numpy()

    return full_mask > out_threshold


if __name__ == '__main__':
    args = parse_args()
    Messages = post_messages(args.msgrest)
    print('Called with args:')
    print(args)
    DirAll(args.image_dir)
    image_list.sort()

    # Load model
    model = init_model(args.checkpoint, device='cuda:' + args.gpu)

    if not os.path.exists(args.output_dir):
        os.makedirs(args.output_dir)

    with open(args.output_dir + "result.json", "w", encoding="utf-8") as f:
        dic = {}
        tmpCount = 0
        for img_path in image_list:
            try:
                tmpCount = tmpCount + 1
                name = os.path.basename(img_path)
                img = Image.open(img_path).convert('L')
                result = predict_img(net=model, full_img=img, device='cuda:'+args.gpu)
                contours = measure.find_contours(result, 0.5)

                mask_list = []
                count =0
                for i in range(len(contours)):
                    if len(contours[i]) > thr:
                        mask = {}
                        mask['class_name'] = 'lung'
                        mask['id'] = str(count)
                        tmp = list(np.reshape(contours[i], [-1]))[::-1]
                        mask['mask'] = tmp
                        count = count + 1
                        mask_list.append(mask)

                dic[name] = mask_list
                if args.msgrest:
                    #print("send http message")
                    msg={}
                    msg['type']="15"  #label lung picture
                    msg['taskid']= args.taskid
                    msg['index']= str(tmpCount)
                    msg['total']= str(len(image_list))
                    msg['filename'] = name
                    data_json = json.dumps(msg)
                    Messages.post(data_json)

                    print("index=" + str(i) + " file=" +  name + "  mask num=" + str(len(mask_list)))
            except (Exception):
                print('detection Failed', os.path.basename(img))
        print(json.dumps(dic), file=f)
    








