import React from 'react';

export class ProductRowComponent extends React.Component{
    constructor(props) {
        super(props);

        this.state = {
            quantity: 0,
            selected: false
        };

        this.onChange = this.onChange.bind(this);
        this.onClick = this.onClick.bind(this);
        this.onRowClick = this.onRowClick.bind(this);
        this.onSelectChange = this.onSelectChange.bind(this);
    }

    onChange(e) {
        this.setState({
            quantity: e.target.value
        });
    }

    onSelectChange(e) {
        this.setState({
            selected: e.target.value
        });
        // Pass the name of the selected item...this might be kind of fragile.
        this.props.selectedFunc(e.target.name);
    }

    onClick(event) {
        event.stopPropagation();
        if (this.props.onClickHandler) {
            this.props.onClickHandler(this.props.item);
        }
        return false;
    }

    onRowClick(event) {
        event.stopPropagation();
        if (this.props.onRowClickHandler) {
            this.props.onRowClickHandler(this.props.item);
        }
        return false;
    }

    render() {
        let row = this.props.fields.map((field, indx) => {
            if (field.fieldName === 'deleteButton') {
                return <td style={field.style} key={indx}>{field.dataPrefix}<button style={{backgroundColor: this.props.buttonColor, color: this.props.textColor}} onClick={() => {
                    this.props.deleteFunc({
                        item: JSON.parse(JSON.stringify(this.props.item))
                    });
                }}>Remove From Page</button></td>;
            }
            else if (field.fieldName === 'quantityInput') {
                return <td key={indx} style={field.style}><input type="number" style={{width: '20px'}} name={this.props.item.name} value={this.state.quantity} onChange={this.onChange} /></td>;
            }
            else if (field.fieldName === 'selectCheckbox') {
                return <td key={indx} style={field.style}><input type="checkbox" style={{width: '20px'}} name={this.props.item.id} value={this.state.selected} onChange={this.onSelectChange} /></td>;
            }
            else if (field.fieldName === 'state') {
                return <td style={field.style} key={indx}><img src={require('../../images/status_' + this.props.item[field.fieldName].toLowerCase() + '.png')} /></td>;
            }
            else {
                if (field.useOnClickHandler) {
                    return <td style={field.style} key={indx}><a onClick={this.onClick}>{field.dataPrefix}{this.props.item[field.fieldName]}{field.dataSuffix}</a></td>;
                }
                else {
                    return <td style={field.style} key={indx}>{field.dataPrefix}{this.props.item[field.fieldName]}{field.dataSuffix}</td>;
                }
            }
        });
        return <tr onClick={this.onRowClick} id={this.props.item.id} className={this.props.rowIndex % 2 === 0 ? 'evenRow' : 'oddRow'} key={this.props.rowIndex}>
            {row}
        </tr>;
    }
}
