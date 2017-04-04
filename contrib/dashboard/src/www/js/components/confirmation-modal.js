/*global $:true*/
import React from 'react';
import { Modal, Button } from 'react-bootstrap';

export class ConfirmationModalComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            showConfirmationFlag: this.props.showConfirmation
        };

        this.close = this.close.bind(this);
        this.cancel = this.cancel.bind(this);
    }

    componentDidMount() {
        this.setState({showConfirmationFlag: this.props.showConfirmation});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({ showConfirmationFlag: nextProps.showConfirmation });
    }

    close() {
        this.props.okFunc();
        this.setState({showConfirmationFlag: false});
    }

    cancel() {
        this.setState({showConfirmationFlag: false});
    }

    render() {
        return <Modal show={this.state.showConfirmationFlag} onHide={this.cancel}>
            <Modal.Header closeButton>
                <Modal.Title>{this.props.title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div>{this.props.confirmationPrompt}</div>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={this.cancel}>Cancel</Button>
                <Button onClick={this.close}>OK</Button>
            </Modal.Footer>
        </Modal>;
    }
}
