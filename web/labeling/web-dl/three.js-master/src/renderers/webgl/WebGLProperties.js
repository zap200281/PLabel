/**
 * @author fordacious / fordacious.github.io
 */

function WebGLProperties() {
// WeakMap：JavaScript的一个构造函数   查MDN   一组键/值对的集合
	var properties = new WeakMap();

	function get( object ) {
		// WeakMap.prototype.get(key)
		// 返回key关联对象, 或者 undefined(没有key关联对象时)。
		// get() 方法返回  WeakMap 指定的元素。  参数：想要从 WeakMap 获取的元素的键  .get(key)
		var map = properties.get( object );

		if ( map === undefined ) {
// 如果map未定义，就给他设置一个空的的对象作为属性值
			map = {};
			// set() 方法根据指定的key和value在 WeakMap对象中添加新/更新元素
			// key 必须的。是要在WeakMap 对象中添加/更新元素的key部分。
			// value  必须的。是要在WeakMap 对象中添加/更新元素的value部分
			properties.set( object, map );

		}

		return map;

	}

	function remove( object ) {
// delete() 方法可以从一个 WeakMap 对象中删除指定的元素。
		properties.delete( object );

	}

	function update( object, key, value ) {
// 设置properties对象的属性的属性值(对象)的某个属性的值
		properties.get( object )[ key ] = value;

	}

	function dispose() {
// 新建一个
		properties = new WeakMap();

	}

	return {
		get: get,
		remove: remove,
		update: update,
		dispose: dispose
	};

}


export { WebGLProperties };
