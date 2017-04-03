/*global $:true*/
import React from 'react';

import { ItemTableComponent } from './item-table';
import { ProductRowComponent } from './product-row';
import { Modal, Button } from 'react-bootstrap';
const helpers = require('../helpers.js');

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
            textAlign: 'center'
        },
        style: {
            paddingLeft: '10px',
            textAlign: 'center'
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
            selectedExperiment: {
                buckets: []
            },
            prioritizedExperiments: [],
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
        this.showMutualExclusions = this.showMutualExclusions.bind(this);
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

        this.setState({
            applicationName: nextProps.applicationName
        });

        helpers.doWasabiOperation('/api/v1/applications/%APPLICATION_NAME%/pages',
            {
                'APPLICATION_NAME': nextProps.applicationName
            }
        ).then(items => {
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
        helpers.doWasabiOperation('/api/v1/experiments/%ID%/pages/%PAGE_NAME%',
            {
                method: 'DELETE',
                'ID': options.item.id,
                'PAGE_NAME': this.state.pageName
            }
        ).then(() => {
            this.refreshExperimentsInPageList(this.state.pageName);
        });
    }

    refreshExperimentsInPageList(pageName) {
        helpers.doWasabiOperation('/api/v1/applications/%APPLICATION_NAME%/priorities',
            {
                'APPLICATION_NAME': this.state.applicationName
            }
        ).then(prioritizedExperiments => {
            this.setState({
                prioritizedExperiments: prioritizedExperiments.prioritizedExperiments
            });
            helpers.doWasabiOperation('/api/v1/experiments/applications/%APPLICATION_NAME%/pages/%PAGE_NAME%',
                {
                    'APPLICATION_NAME': this.state.applicationName,
                    'PAGE_NAME': pageName
                }
            ).then(items => {
                if (items && !items.hasOwnProperty('error')) {
                    for (let i=0; i < items.experiments.length; i++) {
                        items.experiments[i].samplingPercent = items.experiments[i].samplingPercent * 100;
                        items.experiments[i].allocationPercent = items.experiments[i].allocationPercent * 100;
                        // Add the priority
                        for (let j=0; j < this.state.prioritizedExperiments.length; j++) {
                            if (items.experiments[i].id === this.state.prioritizedExperiments[j].id) {
                                // Found it, save the priority
                                items.experiments[i].priority = this.state.prioritizedExperiments[j].priority;
                            }
                        }
                    }
                    items.experiments.sort(function(a, b) {
                        return a.priority - b.priority;
                    });
                    this.setState({
                        items: items.experiments.concat()
                    });

                    helpers.doWasabiOperation('/api/v1/experiments',
                        {}
                    ).then(results => {
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
        for (let i = 0; i < this.state.selectedItems.length; i++) {
            helpers.doWasabiOperation('/api/v1/experiments/%ID%/pages',
                {
                    'ID': this.state.selectedItems[i]
                },
                {
                    pages: [{
                        name: this.state.pageName,
                        allowNewAssignment: true
                    }]
                }
            ).then(() => {
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

    showMutualExclusions(item) {
        const $list = $('#pageExperimentsList'),
            $row = $list.find('#' + item.id),
            $rows = $list.find('tr');

        // highlight mutually exclusive experiments
        $rows.removeClass('hilite active');
        $row.addClass('active');

        helpers.doWasabiOperation('/api/v1/experiments/%ID%/exclusions/?showAll=true&exclusive=true',
            {
                'ID': item.id
            }
        ).then(meExperiments => {
            for (var i = 0; i < meExperiments.experiments.length; i++) {
                for (var j = 1; j < $rows.length; j++) {
                    if ($rows.eq(j).attr('id') === meExperiments.experiments[i].id) {
                        $rows.eq(j).addClass('hilite');
                    }
                }
            }
        });
    }

    openExperimentModal(item) {
        helpers.doWasabiOperation('/api/v1/experiments/%ID%/buckets',
            {
                'ID': item.id
            }
        ).then(results => {
            item.buckets = results.buckets;
            this.setState({
                selectedExperiment: item,
                showExperimentModal: true
            });
        });
    }

    render() {
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
                <ItemTableComponent tableId={'pageExperimentsList'} fields={this.state.myFields} buttonColor={this.state.buttonColor} textColor={this.state.textColor} onClickHandler={this.openExperimentModal} onRowClickHandler={this.showMutualExclusions} deleteFunc={this.deleteExperimentFromPage} items={this.state.items} query={this.state.query} />
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
                                    <td>&nbsp;</td><td>Name</td><td>Sampling %</td><td>Actual %</td><td style={{textAlign: 'center'}}>Status</td>
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
                        <div>Buckets:</div>
                        {this.state.selectedExperiment.buckets.map((item, index) => <div key={index}>{item.label} {item.allocationPercent * 100}%</div>)}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.closeExperimentModal}>Close</Button>
                    </Modal.Footer>
                </Modal>

        </div>;
    }
}
