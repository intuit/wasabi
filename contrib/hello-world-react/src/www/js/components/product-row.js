import React from 'react';

export class ProductRowComponent extends React.Component{
    constructor(props) {
        super(props);

        this.state = {
            quantity: 0
        };

        this.onChange = this.onChange.bind(this);
    }

    onChange(e) {
        this.setState({
            quantity: e.target.value
        });
    }

    render() {
        let row = this.props.fields.map((field, indx) => {
            if (field.fieldName === 'buyButton') {
                return <td style={field.style} key={indx}>{field.dataPrefix}<button style={{backgroundColor: this.props.buttonColor, color: this.props.textColor}} onClick={() => {
                    this.props.buyFunc({
                        item: JSON.parse(JSON.stringify(this.props.item)),
                        quantity: this.state.quantity
                    });
                    this.setState({quantity: 0});
                }}>Buy</button></td>;
            }
            else if (field.fieldName === 'quantityInput') {
                return <td key={indx} style={field.style}><input type="number" style={{width: '20px'}} name={this.props.item.name} value={this.state.quantity} onChange={this.onChange} /></td>;
            }
            else {
                return <td style={field.style} key={indx}>{field.dataPrefix}{this.props.item[field.fieldName]}</td>;
            }
        });
        return <tr className={this.props.rowIndex % 2 === 0 ? 'evenRow' : 'oddRow'} key={this.props.rowIndex}>
            {row}
        </tr>;
    }
}
