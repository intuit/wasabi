import React from 'react';

import { PagesPageComponent } from './pages-page';
import { StatePageComponent } from './state-page';
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs';

export class TabPageComponent extends React.Component{
    constructor(props) {
        super(props);

        this.state = {
            applicationName: this.props.applicationName
        };
    }

    handleSelect(index, last) {
        // Nothing to do
    }

    render() {
        return (
            <Tabs
                onSelect={this.handleSelect}
                forceRenderTabPanel={true}
            >
                <TabList>
                    <Tab>State</Tab>
                    <Tab>Page</Tab>
                </TabList>

                <TabPanel>
                    <StatePageComponent applicationName={this.props.applicationName} />
                </TabPanel>
                <TabPanel>
                    <PagesPageComponent applicationName={this.props.applicationName} />
                </TabPanel>
            </Tabs>
        );
    }
}
