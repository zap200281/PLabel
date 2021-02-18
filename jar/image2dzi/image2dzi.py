from flask import Flask, abort, make_response, render_template, url_for, request, Response
from io import BytesIO
import os
import pyvips
import json
import requests
from logging.handlers import TimedRotatingFileHandler
import logging
import time
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


app = Flask(__name__)
app.config.from_object(__name__)
app.config.from_envvar('DEEPZOOM_MULTISERVER_SETTINGS', silent=True)
log = logging.getLogger('werkzeug')

lock = False

class ProgressBar():
    run=0
    eta=0
    tpels=0
    npels=0
    percent=0

progressbar = ProgressBar()

def progress_print():
    if lock != False:
        res = json.dumps({'isbusy':True,'run':progressbar.run, 'eta':progressbar.eta, 'tpels':progressbar.tpels, 'npels':progressbar.npels, 'percent':progressbar.percent})
    else:
        res = json.dumps({'isbusy':False,'run':progressbar.run, 'eta':progressbar.eta, 'tpels':progressbar.tpels, 'npels':progressbar.npels, 'percent':progressbar.percent})
    return bytes(res,encoding='utf-8')

def preeval_cb(image, progress):
    with open("progress",'a') as f:
        f.write(json.dumps({'isbusy':lock,'run':progress.run, 'eta':progress.eta, 'tpels':progress.tpels, 'npels':progress.npels, 'percent':progress.percent})+'\n')


def eval_cb(image, progress):

    with open("progress",'a') as f:
        f.write(json.dumps({'isbusy':lock,'run':progress.run, 'eta':progress.eta, 'tpels':progress.tpels, 'npels':progress.npels, 'percent':progress.percent})+'\n')


def posteval_cb(image, progress):
    # progressbar.run = progress.run
    # progressbar.eta = progress.eta
    # progressbar.tpels = progress.tpels
    # progressbar.npels = progress.npels
    # progressbar.percent = progress.percent
    with open("progress",'a') as f:
        f.write(json.dumps({'isbusy':lock,'run':progress.run, 'eta':progress.eta, 'tpels':progress.tpels, 'npels':progress.npels, 'percent':progress.percent})+'\n')

def optimistic_lock(file):
    if os.path.exists('progress'):  # 如果进度文件存在
        num1 = 0
        num2 = 0
        num3 = 0
        with open("progress",'r') as f:
            lines = f.readlines() #读取所有行
            num1 = len(lines)
        time.sleep(0.5)
        with open("progress",'r') as f:
            lines = f.readlines() #读取所有行
            num2 = len(lines)
        time.sleep(0.5)
        with open("progress",'r') as f:
            lines = f.readlines() #读取所有行
            num3 = len(lines)
        if num1 == num2 and num2 == num3:
            os.remove(file)
            return True
        else:
            return False
    return True

     
# def progress_kill(image):
#     def preeval_cb(image, progress):
#         pass

#     def eval_cb(image, progress):
#         image.set_kill(True)

#     def posteval_cb(image, progress):
#         pass

#     image.set_progress(True)
#     image.signal_connect('preeval', preeval_cb)
#     image.signal_connect('eval', eval_cb)
#     image.signal_connect('posteval', posteval_cb)


def postMsg(url,code,width,height,level,taskid,resultpath):
    res = {
        'type':code,
        'width':width,
        'height':height,
        'level':level,
        'taskid':taskid,
        'resultpath':resultpath  
    }
    try:
        req = requests.post(url,json=json.dumps(res),timeout=app.config.get('TIMEOUT'),verify=False)
        print(req.status_code)
        app.logger.info(json.dumps({'ret':req.status_code,'msg':'redirect success'}))
        resp = make_response(json.dumps({'ret':200}))
        return resp
    except Exception as e:
        resp = make_response(json.dumps({'ret':410,'msg':'redirect error'}))
        app.logger.error('postMsg():redirect error')
        return resp

def listdir_nohidden(path):
    count = 0
    for f in os.listdir(path):
        if not f.startswith('.'):
            count += 1
    return count

# 检查状态
@app.route('/checkAlive', methods=['GET'])
def checkAlive():
    return Response(json.dumps({'ret':200, 'msg':'is alive'}), mimetype='application/json')

# 查看正在进行的图片处理进度
@app.route('/', methods=['GET'])
def index():
    if os.path.exists('progress'):
        with open('progress','r') as f:
            lines = f.readlines() #读取所有行
            if lines != None:
                last_line = lines[-1]
                return Response(last_line, mimetype='application/json')
            else:
                return Response(json.dumps({'isbusy':False,'run':0, 'eta':0, 'tpels':0, 'npels':0, 'percent':0}), mimetype='application/json')
    else:
        return Response(json.dumps({'isbusy':False,'run':0, 'eta':0, 'tpels':0, 'npels':0, 'percent':0}), mimetype='application/json')

# 测试回调用
@app.route('/getResult', methods=['POST'])
def getResult():
    try:
        resp = make_response('success')
        return resp
    except Exception as e:
        resp = make_response('failed')
        return resp
# post: 请用form表单提交
#   入参: picturepath    图片在服务器上的位置
#        taskId         任务号
#        rest           回调的http地址
#
#   出参: type           消息类型
#         width         图片宽度
#         height        图片高度
#         level         图片层数
#         taskid        回传的taskid
#         resultpath    dzi格式文件及分层文件路径

@app.route('/image2dzi', methods=['GET','POST'])
def image2dzi():
    global lock
    image = None
    if request.method == 'POST' and lock == False:
        try:
            lock = True
            data = dict(request.form)

            image = pyvips.Image.new_from_file(data['picturepath'], access='sequential')
            #image = image1.copy_memory()
            if optimistic_lock('progress') == False:
                app.logger.error('image is on processing!')
                resp = make_response(json.dumps({'ret':500,'msg':'another image is on processing!'}))
                return resp

            image.set_progress(True)
            image.signal_connect('preeval', preeval_cb)
            image.signal_connect('eval', eval_cb)
            image.signal_connect('posteval', posteval_cb)

            image.dzsave(data['picturepath'].split('/')[-1], overlap = app.config.get("OVERLAP"), tile_size = app.config.get('TILE_SIZE'))

            resp = make_response(json.dumps({'ret':200,'msg':'dzi file created'}))
            resp.mimetype = 'application/json'

            dzipath = os.getcwd()+'/'+data['picturepath'].split('/')[-1]+'_files'
            datapath = {'dzipath':dzipath, 'tilepath':os.getcwd()+'/'+data['picturepath'].split('/')[-1]+'.dzi'}
            postMsg(data['rest'],200,image.width,image.height,listdir_nohidden(os.getcwd()+'/'+data['picturepath'].split('/')[-1]+'_files'),data['taskId'],datapath)
            lock = False

            return resp

        except Exception as e:           
            lock = False
            app.logger.error(e.message)
            if e.message.startswith('unable to call dzsave'):
                resp = make_response(json.dumps({'ret':503,'msg':'file aleady existed!'}))
            else:
                resp = make_response(json.dumps({'ret':504,'msg':e.message}))
            return resp

    if lock == True:
        app.logger.error('image is on processing!')
        resp = make_response(json.dumps({'ret':500,'msg':'another image is on processing!'}))
        return resp

    if request.method == 'GET':
        return Response(progress_print(), mimetype="text/event-stream")


if __name__ == '__main__':
    handlers = TimedRotatingFileHandler('image2dzi.log',when='D',interval=1,backupCount=15,encoding='UTF-8',delay=False,utc=False)
    formatter = logging.Formatter(
        "[%(asctime)s] {%(pathname)s:%(lineno)d} %(levelname)s - %(message)s")
    handlers.setFormatter(formatter)
    log.setLevel(logging.INFO)
    log.addHandler(handlers)
    app.logger.setLevel(logging.INFO)
    app.logger.addHandler(handlers)

    app.config.from_pyfile('config.ini', silent=True)
    app.debug = False
    app.reloader = False
    app.run(host=app.config.get('HOST'), port=app.config.get('PORT'), threaded = False, processes=3)
