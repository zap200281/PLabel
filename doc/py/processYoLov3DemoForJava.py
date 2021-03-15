########################################################
													   #
													   #
########################################################

'''

STRUCTURE:

bus_number/date/pos/name,LABEL,*.avi
						   |
						 /name
						 |	  |
					 /images  /labels


'''



import os
import cv2
import json
import argparse
import requests



def yolo_bbx(idir,odir,gpu):
    #idir = os.path.join(path,name)
   # odir = os.path.join(path,'LABEL',name)
    os.system('cd /detection_tool/YOLO-master/darknet && \
    CUDA_VISIBLE_DEVICES={} \
    ./darknet detect cfg/yolov3.cfg yolov3.weights \
    -idir {} \
    -odir {}'.format(gpu,idir+'/',odir+'/'))

def remove_img_txt(path):
    n = 0
    #path = os.path.join(path,'LABEL',name)
    #img_file = os.path.join(path,'images')
    #txt_file = os.path.join(path,'labels')
    txt_files = os.listdir(path)
    for item in txt_files:
        t = os.path.join(path, item)
        if os.path.getsize(t) == 0:
            n += 1
           # image = os.path.join(img_file,item[:-4]+'.jpg') # note: bbx_image name is **.pn.jpg
            txt = os.path.join(path,item)
            #os.remove(image)
            os.remove(txt)
            print('remove {} '.format(txt))
    print('total removed are {}'.format(n))

def mark(path,name,date,pos):
    txt_file = os.path.join(path,'LABEL',name,'labels')
    jpg_file = os.path.join(path,name) # OG images file
    jpg_file_des = os.path.join(path,'LABEL',name,'images/')
    txt_list = os.listdir(txt_file)
    txt_list.sort()

    for i,item in enumerate(txt_list):
        image_name = item[:-4] + 'g' # original image xxx.png
        im = cv2.imread(os.path.join(jpg_file,image_name))
        #print(im.shape)
        tid = open(os.path.join(txt_file,item),'r') # tid is label txt file
        txt_content = tid.readlines()
        tid.close()
        font = cv2.FONT_ITALIC
        for j, p in enumerate(txt_content): # p is coordinate in each line
            bbx = p.strip().split()
            cv2.rectangle(im, (int(bbx[0]), int(bbx[1])), (int(bbx[2]), int(bbx[3])),(0,0,255),3)
            cv2.putText(im, str(j), (int(bbx[0])+10,int(bbx[1])+30),font,1,(0,0,255),3)
        cv2.imwrite(jpg_file_des+item[:-3]+'jpg', im)
        if i % 500 ==0:
            print(i)
    print('{} completed'.format(path))



class post_messages:
    def __init__(self,urls):
        self.urls = urls
        
    def post(self, datajson):
        #print(datajson)
        response = requests.post(self.urls + '/api/message/', json=datajson)
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

imagetxt_list = []; 
image_list=[];
def DirAll(pathName,image_list):
    if os.path.exists(pathName):
        fileList = os.listdir(pathName)
        for f in fileList:
            f=os.path.join(pathName, f)
            if os.path.isdir(f):
                DirAll(f,image_list)
            else:
                dirName = os.path.dirname(f)
                baseName = os.path.basename(f)
                if dirName.endswith(os.sep):
                    image_list.append(dirName+baseName)
                else:
                    image_list.append(dirName+os.sep+baseName)




if __name__=='__main__':
	
    args = parse_args()
    Messages = post_messages(args.msgrest)
    print('Called with args:')
    print(args)
    
    
    idir = args.image_dir
    #idir ='D:/wjr_work/traffic_label/soft_code/detection_tool(0)/data/'
    odir = args.output_dir
   # odir = 'D:/wjr_work/traffic_label/soft_code/detection_tool(0)/Result/'
    if not os.path.exists(os.path.join(odir)):
        os.makedirs(os.path.join(odir))

    yolo_bbx(idir,odir,args.gpu)
    remove_img_txt(odir)
    
    DirAll(idir,image_list)
    DirAll(odir,imagetxt_list)
    imagetxt_list.sort()
    #print("imagetxt_list:",imagetxt_list)
    with open(odir + "result.json", "w", encoding="utf-8") as f:
        dic = {}
        tmpCount = 0;
        for txt in imagetxt_list:
            try:
                if not txt.endswith(".txt"):
                    continue
                img = idir+os.path.basename(txt.split(".")[0]+".jpg")
                tmpCount = tmpCount + 1
#                print("imag:",img)
                name = os.path.basename(img)
#                print("name:",name)
                if not os.path.exists(img):
                    continue
                
                cvimg = cv2.imread(img)
                                   
                ##result = inference_detector(model, img)
                # or save the visualization results to image files
                 
                #print(type(result))
                #print(type(model.CLASSES))
                #print(result)
                #print("=================")
                
                ##show_result(img, result, model.CLASSES, show=False, out_file=args.output_dir + name,score_thr= score_thr)
                #print(model.CLASSES)
                
                tid = open(txt,'r') # tid is label txt file
                txt_content = tid.readlines()
                tid.close()
                
                box_list = []
                count =0
                for i in range(len(txt_content)):
                    row = txt_content[i].strip().split(" ")
                    #print("rowrw:",row)
                    box = {}
                    box['class_name'] = 'person'
                    box['id'] = str(count)
                    #print("row",row[0])
                    tmp = []
                    xmin = int(row[0])
                    ymin = int(row[1])
                    xmax = int(row[2])
                    ymax = int(row[3])
                    tmp.append(str(xmin))
                    tmp.append(str(ymin))
                    tmp.append(str(xmax))
                    tmp.append(str(ymax))
                    box['box'] = tmp
                    #if row[4] >= score_thr :
                    count = count + 1
                    box['score'] = str(round(float(row[4]),2))
                    box_list.append(box)
                        #cut picture
                    if box['class_name'] == 'person' and  args.reid :
                       crop = cvimg[ymin:ymax,xmin:xmax]
                      # cv2.imshow("imge",cvimg)
                      # cv2.waitKey(10)
                       tmpName = odir + "/reid/" + name.replace(".jpg", "_" + str(count) + ".jpg")
                       if not os.path.exists(os.path.join(odir,"reid")):
                         os.makedirs(os.path.join(odir,"reid"))
#                       print("tmpName:",tmpName)
                       cv2.imwrite(tmpName,crop)
                               
                
                dic[name] = box_list
               # print('dic:',dic)
                if args.msgrest:
                   #print("send http message")
                   msg={}
                   msg['type']="1"  #label picture
                   msg['taskid']=args.taskid
                   msg['index']= str(tmpCount)
                   msg['total']= str(len(image_list))
                   msg['filename'] = name
                   data_json = json.dumps(msg)
                   Messages.post(data_json)
                   
                print("index=" + str(i) + " file=" +  name + "  box num=" + str(len(box_list)))
            except (Exception):
                print('detection Failed', os.path.basename(img))
        print(json.dumps(dic), file=f)
        

