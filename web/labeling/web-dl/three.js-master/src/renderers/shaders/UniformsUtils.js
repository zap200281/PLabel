/**
 * Uniform Utilities
 */

var UniformsUtils = {

	merge: function ( uniforms ) {

		var merged = {};

		for ( var u = 0; u < uniforms.length; u ++ ) {
// 遍历参数uniforms的所有元素  拷贝 给对象本身
			var tmp = this.clone( uniforms[ u ] );

			for ( var p in tmp ) {

				merged[ p ] = tmp[ p ];

			}

		}

		return merged;

	},

	clone: function ( uniforms_src ) {

		var uniforms_dst = {};
// for...in 语句用于遍历数组或者对象的属性   遍历merge参数的元素对象
		for ( var u in uniforms_src ) {
// u:属性名的字符串   [ u ] 下标属性名  访问对象的属性，返回属性值  重置属性值
			uniforms_dst[ u ] = {};//uniforms_dst对象多了一个键值对 默认是null？
// 继续遍历对象
			for ( var p in uniforms_src[ u ] ) {

				var parameter_src = uniforms_src[ u ][ p ];//获得属性的属性的属性值
// 判断value的值是否是一下枚举的之一  排除光照
				if ( parameter_src && ( parameter_src.isColor ||
					parameter_src.isMatrix3 || parameter_src.isMatrix4 ||
					parameter_src.isVector2 || parameter_src.isVector3 || parameter_src.isVector4 ||
					parameter_src.isTexture ) ) {

					uniforms_dst[ u ][ p ] = parameter_src.clone();//克隆颜色对象、矩阵对象、向量对象.....等threejs的对象
// 光照对象的vlaue属性的值是：Array
				} else if ( Array.isArray( parameter_src ) ) {

					uniforms_dst[ u ][ p ] = parameter_src.slice();//从原数组截取一段数组，不改变原数组，如果没有参数，返回的新数组包含原来数组的全部元素  类似上面只复制  不改变被复制的对象

				} else {
// 如果不是数组或threejs的某种对象，而是null、小数、整数等JavaScript的数据类型，直接赋值就可以
					uniforms_dst[ u ][ p ] = parameter_src;

				}

			}

		}

		return uniforms_dst;

	}

};


export { UniformsUtils };
