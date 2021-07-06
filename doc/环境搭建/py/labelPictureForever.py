# --------------------------------------------------------
# Tensorflow Faster R-CNN
# Licensed under The MIT License [see LICENSE for details]
# Written by Jiasen Lu, Jianwei Yang, based on code from Ross Girshick
# --------------------------------------------------------

import os
import argparse
import glob
from mmdet.apis import init_detector, inference_detector, show_result
import json
import cv2
import requests
import time
import pdb
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

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
                        default='model/epoch_1_fasterrcnn.pth', type=str)
    parser.add_argument('--image_dir', dest='image_dir',
                        help='directory to load images for demo',
                        default="images")
    parser.add_argument('--output_dir', dest='output_dir',
                        help='directory to load images for demo',
                        default="output")
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


if __name__ == '__main__':

    args = parse_args()
    Messages = post_messages(args.msgrest)
    print('Called with args:')
    print(args)
    model = init_detector(args.cfg, args.checkpoint, device='cuda:' + args.gpu)
    if not os.path.exists(args.output_dir):
        os.makedirs(args.output_dir)

    print('load model finished.')
    score_thr = 0.45
     
    while True :
        fileList = os.listdir(args.image_dir)
        if fileList:  
            for f in fileList:
                f=os.path.join(args.image_dir, f)
                dirName = os.path.dirname(f)
                baseName = os.path.basename(f)
                img = None
                if dirName.endswith(os.sep):
                    img = dirName+baseName
                else:
                    img = dirName+os.sep+baseName
                if img == 'aa_bb_cc_exit.jpg':
                    print('exit')
                    break
                name = os.path.basename(img)
                print(img)
                cvimg = cv2.imread(img)
                result = inference_detector(model, img)
                box_list = []
                count =0
                for i in range(len(result)):
                    for row in result[i]:
                        #count = count + 1
                        box = {}
                        box['class_name'] = model.CLASSES[i]
                        box['id'] = str(count)

                        tmp = []
                        xmin = int(round(row[0]))
                        ymin = int(round(row[1]))
                        xmax = int(round(row[2]))
                        ymax = int(round(row[3]))
                        tmp.append(str(xmin))
                        tmp.append(str(ymin))
                        tmp.append(str(xmax))
                        tmp.append(str(ymax))
                        box['box'] = tmp
                        if row[4] >= score_thr :
                            count = count + 1
                            box['score'] = str(round(row[4], 2))
                            box_list.append(box)

                if args.msgrest:
                   print("send http message")
                   msg={}
                   msg['type']="11"  #label picture
                   msg['taskid']= args.taskid
                   msg['index']= '1'
                   msg['label_info'] = box_list
                   msg['total']= '1'
                   msg['filename'] = name
                   data_json = json.dumps(msg)
                   Messages.post(data_json)
                os.remove(f)  #delete file
               
        else:
            time.sleep(0.5)#xiu mian
        
       



