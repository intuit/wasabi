import React from 'react';

import { ProductRowComponent } from './product-row';

export class ItemTableComponent extends React.Component{
    constructor(props) {
        super(props);

        this.state = {
        };

        this.doFilter = this.doFilter.bind(this);
    }

    doFilter(element) {
        // Filter the list using the search field input
        return (this.props.query.length <= 0 || element.name.indexOf(this.props.query) >= 0);
    }

    render() {
        return  <div>
            <table style={{width: '500px'}}>
                <thead>
                    <tr>
                        {this.props.fields.map((field, index) => <th style={field.headerStyle} key={index}>{field.name}</th>)}
                    </tr>
                </thead>
                <tbody>
                    {this.props.items.filter(this.doFilter).map((item, index) => <ProductRowComponent key={item.id} item={item} rowIndex={index} fields={this.props.fields} buyFunc={this.props.buyFunc} buttonColor={this.props.buttonColor} textColor={this.props.textColor} />)}
                </tbody>
            </table>
        </div>;
    }
}
