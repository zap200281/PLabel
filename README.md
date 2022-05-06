本开源系统已经迁移到  https://git.openi.org.cn/OpenIOSSG/PLabel  进行开发，这里可以存储超大文件，包括docker镜像，可以直接下载。如果对标注系统有需求的，可以在https://git.openi.org.cn/OpenIOSSG/PLabel 这个地址的“任务”栏创建任务，如果工作量不是很大，可能1、2天之内响应。

# PLabel V3.0
增加如下特性：

1、封装所有配置到docker镜像中，用户拉取镜像，启动容器，运行server.sh就可以完成标注软件启动，直接使用。
2、增加文本标注，可以标注实体及实体关系。
3、可以接入用户自己的标注算法或者模型。参见doc目录下 doc\环境搭建\自行训练的模型接入到半自动标注系统说明.docx
4、提供基于CPU运行的yolov5的目标检测及目标跟踪自动标注算法。
5、解决相关Bug及优化后端性能。

另外，有需求的用户，可以在任务”栏创建任务，如果工作量不是很大，可能1、2天之内响应


# PLabel
半自动标注系统是基于BS架构，纯Web页面操作，由鹏城实验室开源所自主研发，由工程师邹安平维护，集成视频抽帧，目标检测、视频跟踪、ReID分类、人脸检测等算法，实现了对图像，视频的自动标注，并可以对自动算法的结果进行人工标注，最终得到标注结果，同时也可以对视频、图片、医疗（包括dicom文件及病理图像）相关的数据进行人工标注，标注结果支持COCO及VOC格式。支持多人协同标注。 半自动标注系统主要功能有：用户管理，数据集管理，自动标注，人工标注（通用图片、文本实体及实体关系），ReID标注，车流统计，视频标注，医疗CT标注，超大图像标注，模型管理与重训，报表管理。数据标注过程一个非常重要的因素是数据安全，在标注使用中防止数据泄露，采用基于web标注工具是有效避免数据泄露的措施之一。 半自动标注系统以保证性能的情况下最小化人工标注代价为目标，不断提升自动标注效率，减少人工标注和人工参与过程。

# Install
新增最简单的方式安装： 下载此docker镜像：https://git.openi.org.cn/attachments/b11fca77-819e-4737-a3c7-056d26b71738
加载镜像 docker load --input pcl_label_hand_v3.tar
运行容器： docker run --name PLabelHand -p 8008:8008 -p 9009:9000 --shm-size 4G -i -t -v /data1/PLabelHand:/data 09dcbf3d1f58 /bin/bash
注意：09dcbf3d1f58为镜像ID，有可能变化，需要使用docker image list查看修改。必需使用8008端口映射，如果要修改，则需要进入docker容器修改nginx下plabel.conf文件中的端口。9009端口用于将容器minio对外暴露，用于基于CPU的自动算法yolov5_auto_label_v1及yolov5_auto_track_v1镜像使用。

进入容器： docker exec -it PLabelHand /bin/bash
先赋mysql目录权限：chown -R mysql:mysql /usr/local/mysql
再运行系统： ./server.sh 再进行回车，一直到出现命令界面。
在web浏览器中输入： http://ip:8008/ 访问标注系统（注意：ip根据实际替换）。 初始用户名：LabelSystem01 / pcl123456

接入基于CPU的yolov5自动标注算法，下载此docker镜像：https://git.openi.org.cn/attachments/0bd57910-5f3e-486a-b645-c476b0eb1782
加载镜像：docker load --input yolov5_auto_label_v1.tar
运行容器：docker run --name yolov5_auto_label -p 8009:8009 --shm-size 4G -i -t -v /data1/PLabelHand:/data d4e5baa3ffd1 /bin/bash
切换到javaapp目录下，修改application-runtime.properties 里面的IP地址 192.168.62.129 为自己的IP地址，
然后运行：java -jar labelSystemForDocker.jar
此后在界面上新建自动标注算法，就可以选择Yolov5算法了，如需要修改标注类别，目前默认标注为person，则需要修改application-runtime.properties中的exe_script中的--classes 0中的标注类别，0代表person。
(由于有个Bug，需要取  doc\环境搭建\自行训练的模型配置文件\yolo\demoForAgentJava.py  替换容器中  /yolo/yolov5/demoForAgentJava.py  ，才能正常标注无标注目的文件。)

接入基于CPU的yolov5多目标跟踪算法，下载此docker镜像：https://git.openi.org.cn/attachments/6051d5e7-678d-44cd-8dc3-1455205533c7
加载镜像：docker load --input yolov5_auto_track_v1.tar
运行容器：docker run --name yolov5_auto_track_v1 -p 8019:8019 --shm-size 4G -i -t -v /data1/PLabelHand:/data 4b2f8e066e57 /bin/bash
切换到javaapp目录下，修改application-runtime.properties 里面的IP地址 192.168.62.129 为自己的IP地址，
然后运行：java -jar labelSystemForDocker.jar
此后在人工标注及ReID标注界面上选择基于Yolov5的多目标跟踪算法Yolov5 Multi target tracking(Person),如需要修改标注类别，目前默认标注为person，则需要修改application-runtime.properties中的exe_script中的--classes 0中的标注类别，0代表person。

其它安装方式：
参见doc目录下环境搭建word文档，本标注系统将会一直维护下去，有疑问、使用过程发现Bug、或者想要新增功能，都可以直接在“任务”页面创建任务，或者联系zouap@pcl.ac.cn，或者微信：13927449476  工作日一般会及时回复。

# Update
v2要升级，直接在v2容器根目录下执行下面的命令即可（先kill掉java进程）：

git clone https://git.openi.org.cn/OpenIOSSG/PLabel
cp -r PLabel/web /javaapp
cp PLabel/jar/labelSystem.jar /javaapp
rm -rf PLabel
echo “update finish.”

cd /javaapp
java -jar labelSystem.jar

java命令可以增加-Xmx等jvm参数，根据自己的内存自行增加。

# Backup
标注系统的数据主要是两个部分，一个minio存储，一个数据库，备好这两个就可以了。
minio存储数据位置：/data/minio/data/
mysql数据库：mysql -uroot -p 登录之后，进行备份即可。备份脚本如下，由zhblue用户提供：

#!/bin/bash
DATE=`date +%Y%m%d`
OLD=`date -d"1 day ago" +"%Y%m%d"`
OLD3=`date -d"3 day ago" +"%Y%m%d"`
USER=root
PASSWORD=pcl123456
DATABASE="labelsystem"
SYSTEM="plabel"

mysqldump -R --column-statistics=0  $DATABASE -u$USER -p$PASSWORD | bzip2 >/var/backups/db_${DATE}.sql.bz2
if tar cjf /var/backups/${SYSTEM}_${DATE}.tar.bz2 /data /javaapp /var/backups/db_${DATE}.sql.bz2; then
	rm /var/backups/${SYSTEM}_${OLD3}.tar.bz2
	rm /var/backups/db_${OLD}.sql.bz2
fi




# Usage
1、用户管理，只有管理员LabelSystem01帐号可以创建用户，分管理员、标注人员、审核人员三类。 在用户管理中可以对每个标注人员进行分表存储标注数据（适合大批量标注，100W级以上），也可以将所有用户的操作的标注数据都存储在同一张表中。

2、数据集管理
   支持视频(需要ffmpeg能支持的格式)、CT影像(.ima,.dcm)、超大图像(.svs，.tiff)、通用图片（常见的图片格式都能支持）、文本实体及实体关系 5种类型的数据。视频支持抽帧，抽帧可以指定文件名方式，也支持视频合并，4种类型数据都可以支持预览。视频目前只有mp4格式可以在线预览，其它的视频需要抽帧之后才能预览。

（注：视频及超大图像需要docker镜像的支持）

3、自动标注
   自动标注目前集成了人、车的几种开源算法，基于mmdetection平台，搜集所有开源数据集训练得到的目标检测模型，新建自动标注时，需要选择数据集，然后等待自动标注完成后，可以查看标注结果。
   自动标注也可以集成开发者的自己的模型，需要开发者在数据库表中插入对应的模型位置及运行脚本，当然模型的输出结果需要与系统读取的数据格式一致。

4、人工标注
  人工标注可以基于自动标注的结果，也可以对数据集中的数据进行直接标注，包括点、多边形、矩形三种标注。人工标注以任务的方式存在，可以看到当前标注的进度。
  人工标注还支持文本的标注，支持实体及实体关系标注，可以参考帮助文档。
  在人工标注页面的按钮中，还可以进行单目标跟踪、多目标跟踪、自动单个图像目标检测。
  包括复制、粘贴、上下左右移动等功能按钮。
  对于人工标注任务在标注人员标注完成后，还可以转给审核人员进行审核。

5、ReID标注
   可以对多个镜头进行ReID标注，在标注页面可以看到多个镜头的ReID标注，大大提升效率，同时查看ReID结果页面会列出所有的ReID标注结果，还可以直接对结果进行修改，删除。也可以对结果进行导出，包括抠图导出，原图导出，标注导出。

6、车流统计
   操作MP4格式的视频，可以在视频上对车辆行驶方向进行标注。

7、视频标注
   操作MP4格式的视频，可以在视频上对目标进行标注。

8、超大图像标注
   对tiff及svs格式的图像进行标注，集成了openseadragon组件，并进行了二次开发，以便可以进行标注。

9、报表管理
   对标注人员的工作进行度量。

10、操作日志管理
  用户登录、登出日志查看。
  
11、模型重训
  可以基于mmdetection使用用户标注的数据重训人、车的检测模型。

标注请使用chrome浏览器，相关截图可参见doc目录下《半自动标注系统介绍_V1.4》，有疑问可以直接联系zouap@pcl.ac.cn，工作日一般会及时回复。

