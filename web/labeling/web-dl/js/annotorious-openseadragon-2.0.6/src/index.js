import axios from 'axios';
import React from 'react';
import ReactDOM from 'react-dom';
import Emitter from 'tiny-emitter';
import OpenSeadragonAnnotator from './OpenSeadragonAnnotator';
import { Environment, WebAnnotation, setLocale } from '@recogito/recogito-client-core';

import '@recogito/recogito-client-core/themes/default';

class OSDAnnotorious {

  constructor(viewer, conf) {
    const config = conf || {};

    this._app = React.createRef();
    
    this._emitter = new Emitter();

    const viewerEl = viewer.element;
    if (!viewerEl.style.position)
      viewerEl.style.position = 'relative';

    setLocale(config.locale);

    this.appContainerEl = document.createElement('DIV');
    viewerEl.appendChild(this.appContainerEl);

    ReactDOM.render(
      <OpenSeadragonAnnotator 
        ref={this._app}
        viewer={viewer} 
        wrapperEl={viewerEl}
        readOnly={config.readOnly} 
        tagVocabulary={config.tagVocabulary}
        onAnnotationSelected={this.handleAnnotationSelected}
        onAnnotationCreated={this.handleAnnotationCreated} 
        onAnnotationUpdated={this.handleAnnotationUpdated} 
        onAnnotationDeleted={this.handleAnnotationDeleted}
        onMouseEnterAnnotation={this.handleMouseEnterAnnotation}
        onMouseLeaveAnnotation={this.handleMouseLeaveAnnotation} />, this.appContainerEl);
  }

  handleAnnotationSelected = annotation => 
    this._emitter.emit('selectAnnotation', annotation.underlying);

  handleAnnotationCreated = annotation =>
    this._emitter.emit('createAnnotation', annotation.underlying);

  handleAnnotationUpdated = (annotation, previous) =>
    this._emitter.emit('updateAnnotation', annotation.underlying, previous.underlying);

  handleAnnotationDeleted = annotation =>
    this._emitter.emit('deleteAnnotation', annotation.underlying);

  handleMouseEnterAnnotation = (annotation, evt) =>
    this._emitter.emit('mouseEnterAnnotation', annotation.underlying, evt);

  handleMouseLeaveAnnotation = (annotation, evt) =>
    this._emitter.emit('mouseLeaveAnnotation', annotation.underlying, evt);

  /******************/               
  /*  External API  */
  /******************/  

  addAnnotation = annotation =>
    this._app.current.addAnnotation(new WebAnnotation(annotation));

  removeAnnotation = annotation =>
    this._app.current.removeAnnotation(new WebAnnotation(annotation));

  loadAnnotations = url => axios.get(url).then(response => {
    const annotations = response.data.map(a => new WebAnnotation(a));
    this._app.current.setAnnotations(annotations);
    return annotations;
  });

  setAnnotations = annotations => {
    const safe = annotations || []; // Allow null for clearning all current annotations
    const webannotations = safe.map(a => new WebAnnotation(a));
    this._app.current.setAnnotations(webannotations);
  }

  setDrawingTool = shape =>
    this._app.current.setDrawingTool(shape);

  getAnnotations = () => {
    const annotations = this._app.current.getAnnotations();
    return annotations.map(a => a.underlying);
  }

  // Shorthand to for wrapping the annotation
  _wrap = annotationOrId =>
    annotationOrId?.type === 'Annotation' ? new WebAnnotation(annotationOrId) : annotationOrId;

  selectAnnotation = annotationOrId => {
    const selected = this._app.current.selectAnnotation(this._wrap(annotationOrId));
    return selected?.underlying;
  }

  panTo = (annotationOrId, immediately) =>
    this._app.current.panTo(this._wrap(annotationOrId), immediately);

  fitBounds = (annotationOrId, immediately) =>
    this._app.current.fitBounds(this._wrap(annotationOrId), immediately);

  /** Sets user auth information **/
  setAuthInfo = authinfo =>
    Environment.user = authinfo;

  /** Clears the user auth information **/
  clearAuthInfo = () =>
    Environment.user = null;

  /** Sets the current 'server time', to avoid problems with locally-generated timestamps **/
  setServerTime = timestamp => 
    Environment.setServerTime(timestamp);

  destroy = () =>
    ReactDOM.unmountComponentAtNode(this.appContainerEl);

  on = (event, handler) =>
    this._emitter.on(event, handler);

  off = (event, callback) =>
    this._emitter.off(event, callback);

}

export default (viewer, config) =>
  new OSDAnnotorious(viewer, config); 
