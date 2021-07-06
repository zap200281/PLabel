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


if __name__ == '__main__':

    args = parse_args()
    Messages = post_messages(args.msgrest)
    print('Called with args:')
    print(args)
    DirAll(args.image_dir)
    image_list.sort()
    model = init_detector(args.cfg, args.checkpoint, device='cuda:' + args.gpu)
    if not os.path.exists(args.output_dir):
        os.makedirs(args.output_dir)
     
    if args.reid :
        if not os.path.exists(args.output_dir + "/reid/"):
           os.makedirs(args.output_dir + "/reid/")
 
    print('load model finished.')
    score_thr = 0.45 
    
    with open(args.output_dir + "result.json", "w", encoding="utf-8") as f:
        dic = {}
        tmpCount = 0;
        for img in image_list:
            try:
                tmpCount = tmpCount + 1
                #print(img)
                name = os.path.basename(img)
                #print(name)
                cvimg = cv2.imread(img)
                result = inference_detector(model, img)
                # or save the visualization results to image files
                 
                #print(type(result))
                #print(type(model.CLASSES))
                #print(result)
                #print("=================")
                
                show_result(img, result, model.CLASSES, show=False, out_file=args.output_dir + name,score_thr= score_thr)
                #print(model.CLASSES)
                box_list = []
                count =0
                for i in range(len(result)):
                    for row in result[i]:
                        #count = count + 1
                        box = {}
                        box['class_name'] = model.CLASSES[i]
                        box['id'] = str(count)
                        #print(str(int(round(row[0]))))
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
                            #cut picture
                            if box['class_name'] == 'person' and  args.reid :
                               crop = cvimg[ymin:ymax,xmin:xmax]
                               tmpName = args.output_dir + "/reid/" + name.replace(".jpg", "_" + str(count) + ".jpg")
                               cv2.imwrite(tmpName,crop)
                               
                
                dic[name] = box_list
                if args.msgrest:
                   #print("send http message")
                   msg={}
                   msg['type']="1"  #label picture
                   msg['taskid']= args.taskid
                   msg['index']= str(tmpCount)
                   msg['total']= str(len(image_list))
                   msg['filename'] = name
                   data_json = json.dumps(msg)
                   Messages.post(data_json)
                   
                print("index=" + str(i) + " file=" +  name + "  box num=" + str(len(box_list)))
            except (Exception):
                print('detection Failed', os.path.basename(img))
        print(json.dumps(dic), file=f)


