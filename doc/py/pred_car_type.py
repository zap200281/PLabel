import argparse
import os

import mmcv
import numpy as np
import torch
from mmcv.parallel import MMDataParallel, MMDistributedDataParallel
from mmcv.runner import get_dist_info, init_dist, load_checkpoint

from mmdet.apis import multi_gpu_test, single_gpu_test
# from mmcls.core import wrap_fp16_model
from mmdet.datasets import build_dataloader, build_dataset
from mmdet.models import build_detector
import pdb
import pickle
import json
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
    parser = argparse.ArgumentParser(description='mmcls test model')
    parser.add_argument('--cfg', help='test config file path')
    parser.add_argument('--checkpoint', help='checkpoint file')
    parser.add_argument('--out', help='output result file')
    parser.add_argument(
        '--eval',
        type=str,
        nargs='+',
        choices=['proposal', 'proposal_fast', 'bbox', 'segm', 'keypoints'],
        help='eval types')
    parser.add_argument(
        '--gpu_collect',
        action='store_true',
        help='whether to use gpu to collect results')
    parser.add_argument('--tmpdir', help='tmp dir for writing some results')
    parser.add_argument(
        '--launcher',
        choices=['none', 'pytorch', 'slurm', 'mpi'],
        default='none',
        help='job launcher')
    parser.add_argument('--local_rank', type=int, default=0)
    parser.add_argument('--image_dir', type=str)
    parser.add_argument('--output_dir', type=str)
    parser.add_argument('--taskid', type=str)
    parser.add_argument('--msgrest', type=str)
    parser.add_argument('--gpu', type=int,default=0)


    args = parser.parse_args()
    if 'LOCAL_RANK' not in os.environ:
        os.environ['LOCAL_RANK'] = str(args.local_rank)
    return args

def align_box_and_type_color(data, output, ids, filenames, pred=None):
    gt_bboxes = data['gt_bboxes'].data[0]
    pred_type = output['pred_type']
    pred_color = output['pred_color']
    num_bboxes0 = 0
    if pred == None:
        pred = []
    for i in range(len(gt_bboxes)):
        num_bboxes = num_bboxes0 + gt_bboxes[i].size(0)
        p_type = pred_type[num_bboxes0:num_bboxes]
        p_color = pred_color[num_bboxes0:num_bboxes]
        pred += [dict_filename_id_color_type(filenames[i], ids[i], p_color, p_type)]
        num_bboxes0 = num_bboxes
    return pred

def dict_filename_id_color_type(filename, ids, colors, types):
    dict_tmp = {'color':[], 'type':[]}
    dict_tmp['filename'] = filename
    for i, i_d in enumerate(ids):
        c = colors[i]
        t = types[i]
        dict_tmp['color'].append({i_d:c})
        dict_tmp['type'].append({i_d:t})
    return dict_tmp

def main():
    args = parse_args()
    Messages = post_messages(args.msgrest)
    
    cfg = mmcv.Config.fromfile(args.cfg)
    # set cudnn_benchmark
    if cfg.get('cudnn_benchmark', False):
        torch.backends.cudnn.benchmark = True
    cfg.model.pretrained = None
    cfg.data.test.test_mode = True

    # init distributed env first, since logger depends on the dist info.
    if args.launcher == 'none':
        distributed = False
    else:
        distributed = True
        init_dist(args.launcher, **cfg.dist_params)
    # build the dataloader
    cfg.pred_dir = args.image_dir
    cfg.data.pred.img_prefix = args.image_dir
    cfg.data.pred.ann_file = os.path.join(args.image_dir,'ImageSets/Main/test.txt')
    dataset = build_dataset(cfg.data.pred)
    data_loader = build_dataloader(
        dataset,
        imgs_per_gpu=cfg.data.imgs_per_gpu,
        workers_per_gpu=cfg.data.workers_per_gpu,
        dist=distributed,
        shuffle=False)
    model = build_detector(cfg.model, train_cfg=cfg.train_cfg, test_cfg=cfg.test_cfg)
    model.eval()
    fp16_cfg = cfg.get('fp16', None)
    if fp16_cfg is not None:
        wrap_fg16_model(model)
    checkpoint = load_checkpoint(model, args.checkpoint, map_location='cpu') 
    if 'CLASSES' in checkpoint['meta']:
        model.CLASSES = checkpoint['meta']['CLASSES']
    else:
        model.CLASSES = dataset.CLASSES
    outputs  = []
    pred = []
    model = MMDataParallel(model, device_ids=[args.gpu])
    prog_bar = mmcv.ProgressBar(len(dataset))
    with torch.no_grad():
        for i, data in enumerate(data_loader):
            IDs = []
            file_list = []
            output_tmp = model(return_loss=True,**data)
            outputs += [output_tmp]
            for j in range(len(data['img_meta'].data[0])):
                IDs += [data['img_meta'].data[0][j]['IDs']]
                file_list += [data['img_meta'].data[0][j]['filename']]
            pred = align_box_and_type_color(data, output_tmp, IDs, file_list, pred)
            for _ in range(2):
                prog_bar.update()
    if not (args.output_dir is None):
        write_dir = args.output_dir
    else:
        write_dir = os.path.join('./work_dirs','pred_type_color.pth')
    with open(write_dir, 'w') as f:
        json.dump(pred, f)
    print('file saved at: ',write_dir)
    if args.msgrest:
         print("send http message")
         msg={}
         msg['type']="15"  #distiguish picture
         msg['taskid']= args.taskid
         msg['index']= '1'
         msg['label_info'] = pred
         msg['total']= '1'
         data_json = json.dumps(msg)
         Messages.post(data_json)

if __name__ == '__main__':
    main()

