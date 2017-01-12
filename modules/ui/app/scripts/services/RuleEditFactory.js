/*global $:false */
/*global jQuery:false */

'use strict';

angular.module('wasabi.services').factory('RuleEditFactory', ['UtilitiesFactory',
    function (UtilitiesFactory) {
        return {
            types: ['boolean', 'number', 'string'],

            placeholders: {
                boolean: 'e.g., true or false',
                number: 'e.g., 10000',
                string: 'e.g., "iphone6"'
            },

            stringTypeOperators: [
                { menuLabel: 'equals', stringForm: '=', jsonForm: 'equals' },
                { menuLabel: 'does not equal', stringForm: '!=', jsonForm: 'notEquals' },
                { menuLabel: 'equals (same case)', stringForm: '^=', jsonForm: 'stringEqualsExact' },
                { menuLabel: 'matches (regexp)', stringForm: '=~', jsonForm: 'stringMatchesRegEx' },
                { menuLabel: 'does not match', stringForm: '!~', jsonForm: 'stringNotMatchesRegEx' }
            ],

            numberTypeOperators: [
                { menuLabel: 'equals', stringForm: '=', jsonForm: 'equals' },
                { menuLabel: 'does not equal', stringForm: '!=', jsonForm: 'notEquals' },
                { menuLabel: 'is greater than', stringForm: '>', jsonForm: 'greaterThan' },
                { menuLabel: 'is less than', stringForm: '<', jsonForm: 'lessThan' },
                { menuLabel: 'is greater than or equal to', stringForm: '>=', jsonForm: 'greaterThanEquals' },
                { menuLabel: 'is less than or equal to', stringForm: '<=', jsonForm: 'lessThanEquals' }
            ],

            booleanTypeOperators: [
                { menuLabel: 'is', stringForm: '=', jsonForm: 'equals' }
            ],

            operators: function() {
                return {
                    'boolean': this.booleanTypeOperators,
                    'number': this.numberTypeOperators,
                    'string': this.stringTypeOperators
                };
            },

            booleanOperators: [
                { menuLabel: 'and', stringForm: '&', jsonForm: 'and' },
                { menuLabel: 'or', stringForm: '|', jsonForm: 'or' }
            ],

            toggleAdvanced: function(params) {
                if (!params.disableSimple) {
                    // They are currently using the widgets to edit the rule.  Convert to the string form.
                    params.experiment.rule = this.convertRuleControlsToRuleString(params.rules, params.tabs);
                    if (params.experiment.rule === null) {
                        // Conversion error, so can't switch
                        return null;
                    }
                    // This allows an empty string, which happens if they haven't specified a rule, but want to go to the advanced side.
                }
                params.simpleRuleEditing = !params.simpleRuleEditing;
                return params;
            },

            // This function is called to create a rule expression string, like "a > 5 & b < 4", from the widgets
            // in the simple rule editing UI.  Or, actually, from the $scope.rules array, which is bound to those
            // widgets.  This will ignore an empty rule expression (no subject or value).  If one of those has been
            // added, but the other hasn't, we have an incomplete rule, which will display an error.  If we try
            // to validate the value in the expression versus the type and it fails, that is also an error.  The
            // rest of the function converts the rule expressions to their string form.
            convertRuleControlsToRuleString: function(rules, tabs) {
                var ruleString = '';
                function getOpStringForm(opToTest, opsArray) {
                    var search = $.grep(opsArray, function(element) {
                        return element.menuLabel === opToTest;
                    });
                    if (search && search.length > 0) {
                        return search[0].stringForm;
                    }
                    return '';
                }
                // The virtue of using the function, rather than something like $.grep(), is that this
                // will kick out once a matching operator is found.  $.grep() and .filter() both test *all*
                // the operator objects and then return *all* the matches, which should only be one.
                function getOperator(operatorList, operatorString) {
                    for (var i = 0; i < operatorList.length; i++) {
                        if (operatorList[i].menuLabel === operatorString) {
                            return operatorList[i].stringForm;
                        }
                    }
                    return '';
                }

                for (var i = 0; i < rules.length; i++) {
                    // Create the string in order by going through the rules.
                    var nextRule = rules[i];

                    if ((!nextRule.subject || $.trim(nextRule.subject).length <= 0) &&
                            (!nextRule.value || $.trim(nextRule.value).length <= 0)) {
                        // The expression is empty, it's not a valid part of a rule, so ignore it.
                        continue;
                    }

                    if (!nextRule.subject || $.trim(nextRule.subject).length <= 0 ||
                        (typeof(nextRule.value) === 'string' && (!nextRule.value || $.trim(nextRule.value).length <= 0)) ||
                        nextRule.type.length === 0 || nextRule.operator.length === 0) {
                        // Since we passed the rule above, they've started filling out this partial rule, but some
                        // of it is missing.  Show an error and get out.
                        UtilitiesFactory.displayPageError('Incomplete Rule', 'Some parts of a rule expression are missing.');
                        tabs[2].active = true;
                        return null;
                    }

                    var resultObj = UtilitiesFactory.validateRuleChunk(nextRule.value, nextRule.type);
                    if (!resultObj.isValid) {
                        // The ValidateRuleValue directive will have highlighted the bad value inputs.
                        UtilitiesFactory.displayPageError('Invalid Rule Value', 'The value is invalid for the rule type in the highlighted fields.');
                        tabs[2].active = true;
                        return null;
                    }

                    if (nextRule.booleanOperator.length > 0) {
                        // There is an AND or OR between two rule expressions (between the previous one and this one).
                        // Insert the '&' or '|' in the string.
                        ruleString += ' ' + getOpStringForm(nextRule.booleanOperator, this.booleanOperators);
                    }
                    var operatorArray = this.stringTypeOperators;
                    switch (nextRule.type) {
                        case 'number':
                            operatorArray = this.numberTypeOperators;
                            break;
                        case 'boolean':
                            operatorArray = this.booleanTypeOperators;
                            break;
                    }
                    if (ruleString.length > 0) {
                        ruleString += ' ';
                    }
                    // Insert the rest of the expression, converting to the operators in the string.
                    ruleString += nextRule.subject + ' ' + getOperator(operatorArray, nextRule.operator) + ' ';
                    ruleString += nextRule.value;
                }
                return ruleString;
            },

            typeChanged: function(rule, subForm) {
                rule.operator = this.operators()[rule.type][0].menuLabel;
                // In case the value was invalid, and this has made it valid, or vice versa, check now.
                var resultObj = UtilitiesFactory.validateRuleChunk(rule.value, rule.type);
                rule.errorMessage = resultObj.tooltipText;
                subForm.ruleValue.$setValidity('validRuleValue', resultObj.isValid);
                // This is a workaround to get the $dirty set on this field and the form.
                subForm.ruleValue.$setViewValue(subForm.ruleValue.$viewValue);
            },

            // This is called by the UI to add a new, empty rule expression.
            addRule: function(rules) {
                var newRule = {
                    booleanOperator: '',
                    type: 'string',
                    subject: '',
                    operator: 'equals',
                    value: '',
                    showDelete: true,
                    errorMessage: 'Enter a quoted string'
                };
                if (rules.length > 0) {
                    // Adding more than the first rule, need the boolean operator.
                    newRule.booleanOperator = 'and';
                }
                rules.push(newRule);
            },

            // This is called by the UI to remove a rule by deleting it from the rules array.
            removeRule : function(index, rules) {
                if (rules.length === 1) {
                    // Last rule, only clear it out.
                    rules[0] = {
                        booleanOperator: '',
                        type: 'string',
                        subject: '',
                        operator: 'equals',
                        value: '',
                        showDelete: true,
                        errorMessage: 'Enter a quoted string'
                    };
                }
                else {
                    // Need to remove the rule, possibly removing the boolean operator on the first one.
                    rules.splice(index, 1);
                    if (rules.length === 1) {
                        rules[0].booleanOperator = '';
                    }
                }
            },

            determineOperandValue: function(operand, forceConstant) {
                var returnObj = {
                    type: undefined,
                    value: undefined
                };
                if (!forceConstant && operand.hasOwnProperty('attribute')) {
                    // This is an attribute name, we don't know type.
                    returnObj.value = operand.attribute;
                }
                else {
                    // Assume constant
                    if ((typeof operand.constant).toLowerCase() === 'boolean') {
                        returnObj.type = 'boolean';
                    }
                    else if ((typeof operand.constant).toLowerCase() === 'number') {
                        returnObj.type = 'number';
                    }
                    else {
                        returnObj.type = 'string'; // Not one of the other types, assume string.
                    }
                    returnObj.value = operand.constant;
                }
                return returnObj;
            },

            // Given a partial rule in the JSON and the operator, this function populates a row of the widget based
            // simple rule editing UI.  The Angular JS template is set up to create the UI based on the $scope.rules
            // array, so all we need to do is populate another expression object in that array correctly for the
            // rule widgets to appear in the UI.
            populateExpression: function(operator, partialRule) {
                var newExpression = {
                    booleanOperator: '',
                    type: '',
                    subject: '',
                    operator: 'equals',
                    value: '',
                    showDelete: true,
                    errorMessage: 'Value not valid for type'
                };
                var leftOperand = this.determineOperandValue(partialRule[operator][0], false);
                var rightOperand = this.determineOperandValue(partialRule[operator][1], true);

                if (['equals', 'notEquals'].indexOf(operator) >= 0) {
                    // Unfortunately, these operators may be used with any type of expression,
                    // but if so, we already have type from determineOperandValue()
                    newExpression.type = (leftOperand.type ? leftOperand.type : rightOperand.type);
                    var operatorArray = this.numberTypeOperators;
                    if (newExpression.type === 'boolean') {
                        operatorArray = this.booleanTypeOperators;
                    }
                    else if (newExpression.type === 'string') {
                        operatorArray = this.stringTypeOperators;
                    }
                    newExpression.operator = $.grep(operatorArray, function(element) {
                        return element.jsonForm === operator;
                    })[0].menuLabel;
                }
                else if (['greaterThan', 'greaterThanEquals', 'lessThan', 'lessThanEquals'].indexOf(operator) >= 0) {
                    // These operators are only used with number type expressions.
                    newExpression.type = 'number';
                    newExpression.operator = $.grep(this.numberTypeOperators, function(element) {
                        return element.jsonForm === operator;
                    })[0].menuLabel;
                }
                else if (['stringEqualsExact', 'stringMatchesRegEx', 'stringNotMatchesRegEx', 'contains', 'notContains'].indexOf(operator) >= 0) {
                    // These operators are only used with string type expressions.
                    newExpression.type = 'string';
                    newExpression.operator = $.grep(this.stringTypeOperators, function(element) {
                        return element.jsonForm === operator;
                    })[0].menuLabel;
                }
                newExpression.subject = leftOperand.value;
                // In the case of a string expression, we want to show the string in the UI as a quoted string.
                newExpression.value = (newExpression.type === 'string' ? '"' + rightOperand.value + '"': rightOperand.value);
                newExpression.errorMessage = UtilitiesFactory.ruleValueValidationError(newExpression.type);
                return newExpression;
            },

            // Extract the operator from a partial rule JSON.  This is a key in an object, but it should be the
            // only key in that object.
            getOperator: function(ruleJSON) {
                // There should only be one key
                var key = null;
                for (key in ruleJSON) {
                    if (ruleJSON.hasOwnProperty(key)) {
                        break;
                    }
                }
                return key;
            },

            // This populates the simple rule editing widgets, which are basically a set of text inputs and
            // select menus that allow you to specify the name of an attribute, the type of the rule, the
            // comparison operator, and the value of the attribute to test against.  If there are more than one,
            // they are combined with an AND or OR.  The code below takes advantage orf (relies on) the fact
            // that the rule JSON is structured so that if there are multiple rule expressions connected with
            // AND or OR, they will appear in the JSON toward the beginning.
            populatePartialRule: function(partialRule, rules, booleanOp) {
                // There should only be one key
                var key = this.getOperator(partialRule);

                if (key.toLowerCase() === 'or' || key.toLowerCase() === 'and') {
                    // The keyed part of the rule is an array of two expressions.  These might be 'or' or 'and' expressions, though.
                    // If one is, we want to evaluate that, first.
                    var leftPartialRule = partialRule[key][0];
                    var rightPartialRule = partialRule[key][1];
                    var leftOp = this.getOperator(leftPartialRule);
                    var rightOp = this.getOperator(rightPartialRule);

                    if (rightOp.toLowerCase() === 'or' || rightOp.toLowerCase() === 'and') {
                        // If the right op is a boolean expression, they must have used parens, because otherwise
                        // you can't have this form, so we throw an exception that will result in only the "advanced"
                        // rule editor (a textfield) is available.
                        throw 'Bad rule JSON';
                    }
                    if (leftOp.toLowerCase() === 'or' || leftOp.toLowerCase() === 'and') {
                        // It turns out that the case we need to watch out for is when the left side is another boolean expression.
                        // The patterns we support, where there are no parentheses, results in trees where the depth is always to the left.
                        this.populatePartialRule(leftPartialRule, rules, booleanOp);
                        this.populatePartialRule(rightPartialRule, rules, key);
                    }
                    else {
                        // Just evaluate them in order.
                        this.populatePartialRule(leftPartialRule, rules, booleanOp);
                        this.populatePartialRule(rightPartialRule, rules, key);
                    }
                }
                else {
                    // Just an expression, populate it.
                    var expr = this.populateExpression(key, partialRule);
                    if (booleanOp) {
                        expr.booleanOperator = booleanOp;
                    }
                    rules.push(expr);  // Add to end of rules array.
                }
            },

            // Attempt to parse the rule JSON and then populate the widgets.  The parse can fail, even if the
            // rule is valid, because we only support a subset in the widgets of what you can do using the
            // rule string.  If the parse or populate call fail, we'll just toggle over to the textarea with the rule string.
            populateRuleControlsFromJSON: function(jsonRule, rules, params) {
                try {
                    var ruleObject = jQuery.parseJSON(jsonRule);
                    this.populatePartialRule(ruleObject, rules);
                } catch (e) {
                    params.simpleRuleEditing = false;
                    params.disableSimple = true;
                }
                return params;
            },

            checkForRule: function(experiment) {
                return !(experiment && experiment.rule && experiment.rule.length > 0);
            }

        };
    }
]);
