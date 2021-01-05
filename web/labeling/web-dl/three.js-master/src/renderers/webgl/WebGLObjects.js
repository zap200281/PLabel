/**
 * @author mrdoob / http://mrdoob.com/
 */

function WebGLObjects( geometries, info ) {

	var updateList = {};
// 模型作为对象
	function update( object ) {

		var frame = info.render.frame;

		var geometry = object.geometry;
		var buffergeometry = geometries.get( object, geometry );

		// Update once per frame

		if ( updateList[ buffergeometry.id ] !== frame ) {

			if ( geometry.isGeometry ) {
// 从Object3D更新此BufferGeometry的属性。
				buffergeometry.updateFromObject( object );

			}

			geometries.update( buffergeometry );

			updateList[ buffergeometry.id ] = frame;

		}

		return buffergeometry;

	}

	function dispose() {

		updateList = {};

	}

	return {

		update: update,
		dispose: dispose

	};

}


export { WebGLObjects };
