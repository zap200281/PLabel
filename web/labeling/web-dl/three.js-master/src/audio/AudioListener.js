/**
 * @author mrdoob / http://mrdoob.com/
 */

import { Vector3 } from '../math/Vector3.js';
import { Quaternion } from '../math/Quaternion.js';
import { Object3D } from '../core/Object3D.js';
import { AudioContext } from './AudioContext.js';

function AudioListener() {

	Object3D.call( this );

	this.type = 'AudioListener';
// AudioContext封装了原生AudioContext
	this.context = AudioContext.getContext();
// 创建一个GainNode,它可以控制音频的总音量    控制一个固定频率的声音音量变化曲线，模拟琴按键声
	this.gain = this.context.createGain();
// audioCtx.destination返回AudioDestinationNode对象，表示当前audio context中所有节点的最终节点，一般表示音频渲染设备
	this.gain.connect( this.context.destination );

	this.filter = null;

}

AudioListener.prototype = Object.assign( Object.create( Object3D.prototype ), {

	constructor: AudioListener,

	getInput: function () {

		return this.gain;

	},

	removeFilter: function ( ) {

		if ( this.filter !== null ) {

			this.gain.disconnect( this.filter );
			this.filter.disconnect( this.context.destination );
			this.gain.connect( this.context.destination );
			this.filter = null;

		}

	},

	getFilter: function () {

		return this.filter;

	},

	setFilter: function ( value ) {

		if ( this.filter !== null ) {

			this.gain.disconnect( this.filter );
			this.filter.disconnect( this.context.destination );

		} else {

			this.gain.disconnect( this.context.destination );

		}

		this.filter = value;
		this.gain.connect( this.filter );
		this.filter.connect( this.context.destination );

	},

	getMasterVolume: function () {

		return this.gain.gain.value;

	},

	setMasterVolume: function ( value ) {

		this.gain.gain.setTargetAtTime( value, this.context.currentTime, 0.01 );

	},
// 能够做到，比如该对象作为相机对象的子对象存在，当OrbitControls.js改变相机对象相关属性，绘制执行该方法
// 绘制执行该方法：
	updateMatrixWorld: ( function () {

		var position = new Vector3();
		var quaternion = new Quaternion();
		var scale = new Vector3();

		var orientation = new Vector3();

		return function updateMatrixWorld( force ) {

			Object3D.prototype.updateMatrixWorld.call( this, force );
// AudioListener接口表示收听音频场景的独特人员的位置和方向，并用于音频空间化。
// 所有PannerNodes相对于存储在AudioContext.listener属性中的AudioListener进行空间化。
			var listener = this.context.listener;

// 获得对象上方向向量
			var up = this.up;
// Matrix4对象的方法decompose：将这个矩阵分解为平移，四元数和缩放分量。
//执行该方法分解对象的世界矩阵，重置position, quaternion, scale三个变量的值   参考Matrix4.js文件
			this.matrixWorld.decompose( position, quaternion, scale );
// set重置向量，并返回本身，然后通过方法applyQuaternion提取该四元数旋转变换信息，对orientation向量进行旋转变换
			orientation.set( 0, 0, - 1 ).applyQuaternion( quaternion );

			if ( listener.positionX ) {//判断是否使用新的API
// 设置侦听器listener的位置   对象在整个场景中全局位置
// 如果listener没有设置位置属性，直接camera.add( listener )同时该相机对象没有父对象，position的值就是相机对象的position
// 为什么把相机对象的位置所谓监听器的位置：
// 声音上下文的listener属性用来计算声音大小的API  具有距离、方位特性  甚至姿态
// 假设一个很大的场景，在交互的时候，使用一个场景漫游类型的轨迹控件，人的位置就可以视为相机对象的位置
//
// position反应了监听者的位置   可以得到宏观的距离、方向参数
				listener.positionX.setValueAtTime( position.x, this.context.currentTime );
				listener.positionY.setValueAtTime( position.y, this.context.currentTime );
				listener.positionZ.setValueAtTime( position.z, this.context.currentTime );
// forward、up反应的是人听声音的方位   好比转动头耳朵位置发生变化
				listener.forwardX.setValueAtTime( orientation.x, this.context.currentTime );
				listener.forwardY.setValueAtTime( orientation.y, this.context.currentTime );
				listener.forwardZ.setValueAtTime( orientation.z, this.context.currentTime );
				listener.upX.setValueAtTime( up.x, this.context.currentTime );
				listener.upY.setValueAtTime( up.y, this.context.currentTime );
				listener.upZ.setValueAtTime( up.z, this.context.currentTime );

			} else {
// 设置侦听器listener的位置。 请参阅弃用功能以了解为何删除此方法。
				listener.setPosition( position.x, position.y, position.z );
// 设置方向   Orientation：定位、方位、方向
				listener.setOrientation( orientation.x, orientation.y, orientation.z, up.x, up.y, up.z );

			}

		};

	} )()

} );

export { AudioListener };
