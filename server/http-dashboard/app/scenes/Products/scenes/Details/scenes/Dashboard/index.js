import React from 'react';
import PropTypes from 'prop-types';
import {Widgets} from 'components';

class Dashboard extends React.Component {

  static propTypes = {
    widgets: PropTypes.array
  };

  render() {

    const params = {
      id: 0,
    };

    if (!this.props.widgets)
      return null;

    if (!this.props.widgets.length)
      return (<div className="product-no-fields">No Dashboard widgets</div>);

    let widgets = {
      lg: this.props.widgets.map((item) => ({
        ...item,
        i: String(item.id),
        id: String(item.id),
        w: item.width,
        h: item.height,
        x: item.x,
        y: item.y
      }))
    };

    return (
      <Widgets editable={false} data={widgets} params={params} isPreviewOnly={true}/>
    );
  }

}

export default Dashboard;
