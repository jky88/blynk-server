import React from 'react';
import {Link} from 'react-router';

export default class Confirmation extends React.Component {

  static propTypes = {
    router: React.PropTypes.object
  };

  render() {
    return (<div className="confirm-container">
      <div className="form-header">Password change</div>
      <div className="confirm-message">
        <div>Password was changed successfully.</div>

        <Link className="back-to-login" to="/login">Continue</Link>
      </div>
    </div>);
  }
}
