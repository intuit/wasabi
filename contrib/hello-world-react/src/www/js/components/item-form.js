import React from 'react';

export class ItemFormComponent extends React.Component {

	constructor(props) {
		super(props);

		this.state = {
			newColor: ''
		};

		this.onChange = this.onChange.bind(this);
		this.onClick = this.onClick.bind(this);
	}

	onChange(e) {

		this.setState({
			[e.target.name]: e.target.value
		});

	}
	
	onClick() {

		this.props.addItem(this.state.newColor);

		this.setState({
			newColor: ''
		});
	}	


	render() {

		return <form>
			<label>
				New Color:
				<input type="text" name="newColor" value={this.state.newColor} onChange={this.onChange} />
			</label>
			<button type="button" onClick={this.onClick}>Add Color</button>
		</form>;

	}

}