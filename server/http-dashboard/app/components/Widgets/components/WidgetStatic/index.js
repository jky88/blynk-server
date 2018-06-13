import React from 'react';

import {WIDGET_TYPES} from 'services/Widgets';

import PropTypes from 'prop-types';

import {
  LinearWidget,
  // BarChartWidget,
  LabelWidget,
  SwitchWidget,
  SliderWidget
} from 'components/Widgets';

import {
  Slider as SliderDataWrapper,
  Label as LabelDataWrapper,
  LineChart as LineChartDataWrapper,
  Switch as SwitchDataWrapper,
} from 'scenes/Devices/scenes/WidgetDataWrapper';

import _ from 'lodash';

class WidgetStatic extends React.Component {

  static propTypes = {

    fetchData: PropTypes.bool,
    loading: PropTypes.bool,
    isLive: PropTypes.bool,

    widget : PropTypes.object,
    history: PropTypes.oneOfType([
      PropTypes.array,
      PropTypes.object
    ]),
    style  : PropTypes.object,

    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element
    ]),

    deviceId: PropTypes.number,
  };

  constructor(props) {
    super(props);
  }

  shouldComponentUpdate(nextProps) {
    return (
      !_.isEqual(nextProps.widget, this.props.widget) ||
      !_.isEqual(nextProps.loading, this.props.loading) ||
      !_.isEqual(nextProps.isLive, this.props.isLive) ||
      !_.isEqual(nextProps.style, this.props.style) ||
      !_.isEqual(nextProps.deviceId, this.props.deviceId)
    );
  }

  render() {
    const widget = this.props.widget;

    const attributes = {
      key                : widget.id,
      deviceId           : this.props.deviceId,
      data               : widget,
      name               : widget.type + widget.id,
      history            : this.props.history,
    };

    attributes.parentElementProps = {
      id         : widget.type + widget.id,
      style      : this.props.style,
    };

    let pins = [];

    if(widget && Array.isArray(widget.sources)) {
      pins = widget.sources.map((source) => {

        let pin = -1;

        if(source && source.dataStream && typeof source.dataStream.pin !== undefined) {
          pin = source.dataStream.pin;
        }

        return pin;

      }).filter((pin) => pin >= 0);
    }

    const dataWrapperAttributes = {
      type     : widget.type,
      isLive   : this.props.isLive,
      deviceId : this.props.deviceId,
      widgetId : widget.id,
      pins     : pins,
      fetchData: this.props.fetchData
    };

    if (widget.type === WIDGET_TYPES.LINEAR)
      return (
        <LineChartDataWrapper {...dataWrapperAttributes}>
          <LinearWidget {...attributes}/>
        </LineChartDataWrapper>
      );

    if (widget.type === WIDGET_TYPES.BAR)
      return null;
      // return (
        {/*<BarChartWidget {...attributes}/>*/}
      // );

    if (widget.type === WIDGET_TYPES.LABEL)
      return (
        <LabelDataWrapper {...dataWrapperAttributes}>
          <LabelWidget {...attributes}/>
        </LabelDataWrapper>
      );

    if (widget.type === WIDGET_TYPES.SWITCH)
      return (
        <SwitchDataWrapper {...dataWrapperAttributes}>
          <SwitchWidget {...attributes}/>
        </SwitchDataWrapper>
      );

    if (widget.type === WIDGET_TYPES.SLIDER)
      return (
        <SliderDataWrapper {...dataWrapperAttributes}>
          <SliderWidget {...attributes}/>
        </SliderDataWrapper>
      );

  }

}

export default WidgetStatic;
