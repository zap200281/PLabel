import React, { Component } from 'react';
import { Editor } from '@recogito/recogito-client-core';
import OSDAnnotationLayer from './annotations/OSDAnnotationLayer';

export default class OpenSeadragonAnnotator extends Component {

  state = {
    selectedAnnotation: null,
    selectedDOMElement: null
  }

  /** Shorthand **/
  clearState = () => this.setState({
    selectedAnnotation: null,
    selectedDOMElement: null
  });

  componentDidMount() {
    this.annotationLayer = new OSDAnnotationLayer(this.props);
    this.annotationLayer.on('mouseEnterAnnotation', this.props.onMouseEnterAnnotation);
    this.annotationLayer.on('mouseLeaveAnnotation', this.props.onMouseLeaveAnnotation);
    this.annotationLayer.on('select', this.handleSelect);
    this.annotationLayer.on('moveSelection', this.handleMoveSelection);
  }

  componentWillUnmount() {
    this.annotationLayer.destroy();
  }

  handleSelect = evt => {
    const { annotation, element, skipEvent } = evt;
    if (annotation) {
      this.setState({ 
        selectedAnnotation: annotation, 
        selectedDOMElement: element 
      });

      if (!annotation.isSelection && !skipEvent)
        this.props.onAnnotationSelected(annotation.clone());
    } else {
      this.clearState();
    }
  }

  handleMoveSelection = selectedDOMElement =>
    this.setState({ selectedDOMElement });

  /**************************/  
  /* Annotation CRUD events */
  /**************************/  

  onCreateOrUpdateAnnotation = method => (annotation, previous) => {
    this.clearState();    
    this.annotationLayer.deselect();
    this.annotationLayer.addOrUpdateAnnotation(annotation, previous);

    // Call CREATE or UPDATE handler
    this.props[method](annotation, previous?.clone());
  }

  onDeleteAnnotation = annotation => {
    this.clearState();
    this.annotationLayer.removeAnnotation(annotation);
    this.props.onAnnotationDeleted(annotation);
  }

  onCancelAnnotation = () => {
    this.clearState();
    this.annotationLayer.deselect();
  }

  /****************/               
  /* External API */
  /****************/

  addAnnotation = annotation =>
    this.annotationLayer.addOrUpdateAnnotation(annotation.clone());

  removeAnnotation = annotation =>
    this.annotationLayer.removeAnnotation(annotation.clone());

  setAnnotations = annotations =>
    this.annotationLayer.init(annotations.map(a => a.clone()));

  getAnnotations = () =>
    this.annotationLayer.getAnnotations().map(a => a.clone());

  setDrawingTool = shape =>
    this.annotationLayer.setDrawingTool(shape);

  selectAnnotation = arg => {
    const annotation = this.annotationLayer.selectAnnotation(arg);
    
    if (annotation) 
      return annotation.clone();
    else 
      this.clearState(); // Deselect
  }

  panTo = (annotationOrId, immediately) =>
    this.annotationLayer.panTo(annotationOrId, immediately);

  fitBounds = (annotationOrId, immediately) =>
    this.annotationLayer.fitBounds(annotationOrId, immediately);

  render() {
    return (
      this.state.selectedAnnotation && (
        <Editor
          wrapperEl={this.props.wrapperEl}
          annotation={this.state.selectedAnnotation}
          selectedElement={this.state.selectedDOMElement}
          readOnly={this.props.readOnly}
          onAnnotationCreated={this.onCreateOrUpdateAnnotation('onAnnotationCreated')}
          onAnnotationUpdated={this.onCreateOrUpdateAnnotation('onAnnotationUpdated')}
          onAnnotationDeleted={this.onDeleteAnnotation}
          onCancel={this.onCancelAnnotation}>

          <Editor.CommentWidget />
          <Editor.TagWidget vocabulary={this.props.tagVocabulary} />

        </Editor>
      )
    )
  }

}