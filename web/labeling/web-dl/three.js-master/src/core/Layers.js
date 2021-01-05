/**
 * @author mrdoob / http://mrdoob.com/
 */
// 定义一个函数可以把十六进制整数作为参数，比如0xADDFDA,调用函数后可以拆分出来三个值0xAD、0xDF、0xDA，就是颜色的三种成分红绿蓝
// function setHex( hex ) {//hex一个十六进制数，代表颜色值RGB
//
// hex = Math.floor( hex );
//
// this.r = ( hex >> 16 & 255 ) / 255;//获取颜色值R部分
// this.g = ( hex >> 8 & 255 ) / 255;//获取颜色值G部分
// this.b = ( hex & 255 ) / 255;//获取颜色值B部分
//
// return this;
// }


function Layers() {

	this.mask = 1 | 0;

}
// 难道比较的时候是两个对象的mask进行二进制计算   计算结果是true
Object.assign( Layers.prototype, {

	set: function ( channel ) {
// set(3)  左移1位
		this.mask = 1 << channel | 0;

	},

	enable: function ( channel ) {

		this.mask |= 1 << channel | 0;

	},

	toggle: function ( channel ) {

		this.mask ^= 1 << channel | 0;

	},

	disable: function ( channel ) {

		this.mask &= ~ ( 1 << channel | 0 );

	},

	test: function ( layers ) {
// 按位与运算：&   每一位的运算和&&一致
// mask只有一位是1，所以只要值不相等，按位与的结果一定是0.如果相等，返回true
		return ( this.mask & layers.mask ) !== 0;

	}

} );


export { Layers };
