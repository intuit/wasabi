/*global $:true*/
import React from 'react';

import { ItemTableComponent } from './item-table';
import { ProductRowComponent } from './product-row';
import { Modal, Button } from 'react-bootstrap';

const myFields = [
    {
        name: 'Name',
        fieldName: 'label',
        headerStyle: {
            width: '160px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: true
    },
    {
        name: 'Sampling %',
        fieldName: 'samplingPercent',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '%',
        useOnClickHandler: false
    },
    {
        name: 'Actual %',
        fieldName: 'allocationPercent',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '%',
        useOnClickHandler: false
    },
    {
        name: 'State',
        fieldName: 'state',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: false
    // },
    // {
    //     name: 'Cost',
    //     fieldName: 'cost',
    //     headerStyle: {
    //         width: '100px'
    //     },
    //     style: {
    //         textAlign: 'right',
    //         paddingRight: '10px'
    //     },
    //     dataPrefix: '$'
    // },
    // {
    //     name: 'Quantity',
    //     fieldName: 'quantityInput',
    //     headerStyle: {
    //     },
    //     style: {
    //         textAlign: 'center'
    //     },
    //     dataPrefix: ''
    },
    {
        name: ' ',
        fieldName: 'deleteButton',
        headerStyle: {
            width: '50px'
        },
        style: {
            textAlign: 'center'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: false
    }
];

const addDialogFields = [
    {
        name: '',
        fieldName: 'selectCheckbox',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: false
    },
    {
        name: 'Name',
        fieldName: 'label',
        headerStyle: {
            width: '160px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: false
    },
    {
        name: 'Sampling %',
        fieldName: 'samplingPercent',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '%',
        useOnClickHandler: false
    },
    {
        name: 'Actual %',
        fieldName: 'allocationPercent',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '%',
        useOnClickHandler: false
    },
    {
        name: 'State',
        fieldName: 'state',
        headerStyle: {
            width: '60px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: '',
        dataSuffix: '',
        useOnClickHandler: false
    }
];

export class PagesPageComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            items: [],
            experimentsNotInPage: [],
            myFields: myFields,
            addDialogFields: addDialogFields,
            pageName: '',
            query: '',
            pages: [],
            showModal: false,
            showExperimentModal: false,
            selectedItems: [],
            selectedExperiment: {},
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                }
            }
        };

        this.deleteExperimentFromPage = this.deleteExperimentFromPage.bind(this);
        this.handleSelectChange = this.handleSelectChange.bind(this);
        this.refreshExperimentsInPageList = this.refreshExperimentsInPageList.bind(this);
        this.selectedFunc = this.selectedFunc.bind(this);
        this.openExperimentModal = this.openExperimentModal.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
            this.setState({
                session: session
            });
        }

        this.onClick = this.onClick.bind(this);
        this.close = this.close.bind(this);
        this.closeExperimentModal = this.closeExperimentModal.bind(this);
        this.save = this.save.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.hasOwnProperty('applicationName') || nextProps.applicationName === '') {
            return;
        }
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        this.setState({
            applicationName: nextProps.applicationName
        });

        fetch('http://localhost:8080/api/v1/applications/' + nextProps.applicationName + '/pages', {
            method: 'GET',
            headers: headers
        }).then(res => res.json()).then(items => {
            if (items && !items.hasOwnProperty('error')) {
                this.setState({
                    pages: items.pages.concat()
                });

                if (items.pages.length === 0) {
                    this.setState({ items: [] });
                }
            }
        });
    }

    deleteExperimentFromPage(options) {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        fetch('http://localhost:8080/api/v1/experiments/' + options.item.id + '/pages/' + this.state.pageName, {
            method: 'DELETE',
            headers: headers
        }).then(() => {
            this.refreshExperimentsInPageList(this.state.pageName);
        });
    }

    refreshExperimentsInPageList(pageName) {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        fetch('http://localhost:8080/api/v1/experiments/applications/' + this.state.applicationName + '/pages/' + pageName, {
            method: 'GET',
            headers: headers
        }).then(res => res.json()).then(items => {
            if (items && !items.hasOwnProperty('error')) {
                for (let i=0; i < items.experiments.length; i++) {
                    items.experiments[i].samplingPercent = items.experiments[i].samplingPercent * 100;
                    items.experiments[i].allocationPercent = items.experiments[i].allocationPercent * 100;
                }
                this.setState({
                    items: items.experiments.concat()
                });

                fetch('http://localhost:8080/api/v1/experiments', {
                    method: 'GET',
                    headers: headers
                }).then(res => res.json()).then(results => {
                    if (results && !results.hasOwnProperty('error')) {
                        const experimentsNotInPage = [];
                        for (let i=0; i < results.experiments.length; i++) {
                            let found = false;
                            for (let j=0; j < items.experiments.length; j++) {
                                if (results.experiments[i].label === items.experiments[j].label) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                results.experiments[i].samplingPercent *= 100;
                                results.experiments[i].allocationPercent *= 100;
                                experimentsNotInPage.push(results.experiments[i]);
                            }
                        }
                        this.setState({
                            experimentsNotInPage: experimentsNotInPage
                        });
                    }
                });
            }
        });
    }

    handleSelectChange(event) {
        this.setState({pageName: event.target.value});

        if (event.target.value.length === 0) {
            // Resetting to not selected
            this.setState({ items: [] });
            return;
        }

        this.refreshExperimentsInPageList(event.target.value);
    }

    onClick() {
        this.setState({ showModal: true });
    }

    close() {
        this.setState({ showModal: false });
    }

    save() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        for (let i = 0; i < this.state.selectedItems.length; i++) {
            fetch('http://localhost:8080/api/v1/experiments/' + this.state.selectedItems[i] + '/pages', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    pages: [{
                        name: this.state.pageName,
                        allowNewAssignment: true
                    }]
                })
            }).then(() => {
                this.refreshExperimentsInPageList(this.state.pageName);
            });
        }


        this.setState({
            showModal: false,
            selectedItems: []
        });
    }

    selectedFunc(selectedName) {
        this.setState({ selectedItems: this.state.selectedItems.concat(selectedName) });
    }

    closeExperimentModal() {
        this.setState({ showExperimentModal: false });
    }

    openExperimentModal(item) {
        this.setState({
            selectedExperiment: item,
            showExperimentModal: true
        });
    }

    render() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        return <div>
                <section className="pageMenu">
                    Page:     <select value={this.state.pageName} onChange={this.handleSelectChange}>
                                <option value="">Select a page</option>
                                {this.state.pages.map((page, index) => <option value={page.name} key={index}>{page.name}</option>)}
                              </select>
                </section>
                <section className="addButtonArea">
                    <button type="button" onClick={this.onClick}>Add Experiment To Page</button>
                </section>
                <ItemTableComponent fields={this.state.myFields} buttonColor={this.state.buttonColor} textColor={this.state.textColor} onClickHandler={this.openExperimentModal} deleteFunc={this.deleteExperimentFromPage} items={this.state.items} query={this.state.query} />
                <div className="modalWindow">
                    Shown now?
                </div>

                <Modal show={this.state.showModal} onHide={this.close}>
                    <Modal.Header closeButton>
                        <Modal.Title>Add Experiment to Page</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <table style={{width: '100%'}}>
                            <thead>
                                <tr>
                                    <td>Name</td><td>Sampling %</td><td>Actual %</td><td>State</td>
                                </tr>
                            </thead>
                            <tbody>
                                {this.state.experimentsNotInPage.map((item, index) => <ProductRowComponent key={item.id} item={item} rowIndex={index} fields={this.state.addDialogFields} selectedFunc={this.selectedFunc} buttonColor={this.props.buttonColor} textColor={this.props.textColor} />)}
                            </tbody>
                        </table>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.save}>Save</Button>
                        <Button onClick={this.close}>Close</Button>
                    </Modal.Footer>
                </Modal>

                <Modal show={this.state.showExperimentModal} onHide={this.closeExperimentModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>Details</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div>{this.state.selectedExperiment.label}</div>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.closeExperimentModal}>Close</Button>
                    </Modal.Footer>
                </Modal>

        </div>;
    }
}
