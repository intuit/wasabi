# Forming a Segmentation Rule

The Segmentation Rule feature of the AB Testing server allows the experiment owner to target an AB Test to a specific
user group that fall into a certain category. For example, if you want to target an AB Test only to the people whose
salary is greater than $80000 and who live in California, the rule would say salary > 80000 & state = "CA".

## How do I form the rules?

Creating rules is simple. Here are some instructions that will help you understand.

The easiest way to create a rule (unless it is a very large rule) is using the Rule Creation UI.  This is the default UI
you see when you edit an experiment and go to the Segmentation tab:

![Edit Experiment](segmentation/segRuleEdit.png)

By default, you should see the "form view" for entering and editing a rule.  This allows you to enter and edit
segmentation rules without having to know the expression syntax.  (If you don't see the fields and menus shown above in
the Segmentation tab, click on the "Switch to form view..." link on the bottom right.)

So, for example, to enter the rule mentioned above, you would first enter "salary" in the "Parameter name" field, then
select "number" from the drop down.  When you do that, the selections in the next drop down will be valid choices for
number type expressions ("equals", "does not equal", "is greater than", "is less than", "is greater than or equal to",
or "is less than or equal to").  Select "is greater than".  Finally, the value to test against, 80000, is entered in the
final field.

Since we need another rule, "state = CA", we click on "Add rule...".  This adds another row of widgets, including one
that allows you to select "and" or "or" to the left of the second row.  We want to "and" the rule segments, so we leave
the default choice of "and" for the first drop down menu.  We enter "state" for the "Parameter name", select "string"
for the type, select "equals" for the comparison operator, and then enter "CA" for the comparison value.  Note that you
actually need to enter the quotes for the "CA" (the placeholder text gives you a hint for each type of value).

Your segmentation rule tab should now look like this:

![Edit Experiment](segmentation/segRuleEdit3.png)

If you now save your experiment and then edit it again, you can use the "Test rule..." feature.  This is only enabled
after you have saved the segmentation rule because the testing feature actually hits the AB Testing service and uses the
same code that is used when your application calls to assign a user to the experiment.  When you click on that link,
you'll get the following dialog:

![Test Segmentation Rule](segmentation/segRuleTest.png)

Notice that this dialog is created from the rule, e.g., there is a field for a value for "salary" and one for a value
for "state".  You put example values in those fields like what you will be passing for each user and then you can see
if, given those values, the rule would pass or fail.

## What type of comparisons can I have in my rules?

We support a variety of comparison "operators" that can be used to create the rule.  These are what you use to compare
the values you pass in for each user with the value being tested against in the rule.

### For Strings

meaning | operator | note
------- | -------- | ----
equals | = | this operator ignores upper and lower case
does not equal | != | this operator ignores upper and lower case
equals (same case) | ^= | exact matching including upper and lower case
matches (regexp) | =~ | using Java regex syntax*
does not match (regexp) | !~ | using Java regex syntax*



* more specifically, left =~ right is equivalent to the following Java statement: Pattern.matches(right, left) (see here
for more documentation).

Examples for String rules:

`name = "wasabi"`

`name != "wasabi"`

`name =~ "(.*)srikanth(.*)" //This matches all Strings that contain the word Srikanth. The left hand side is the key and
the right hand side is the RegEx pattern. The RegEx is standard Java RegEx.`

`name !~ "(.*)srikanth(.*)"`



### For numerical values:

meaning | operator
------- | --------
equals | =
does not equal | !=
greater than | >
greater than or equal to | >=
less than | <
less than equal to | <=



### For boolean values:

meaning | operator
------- | --------
is | =
is not | !=



### For date values:

meaning | operator | note
------- | -------- | ----
is on, equals | = | dates that exactly match (down to the millisecond)
is before | < | left date is before right date
is after | > | left date is after right date
is not on, does not equal | != | negation of on



The symbols in brackets are the representations of the operators in the logical expression language, referred to in the
UI as the "text view" of the rule. Operators can compare any combination of attributes and constants, so both a
comparison of attributes to constants as well as comparing two different attributes is possible (although that is not
supported by the form view in the UI).

Conditions can be combined using boolean operators: and (&), or (|), not (!): NOTE: This is not available through the
"form view" in the UI.

## What is the Logical Expression Language

NOTE: If you can fit your rules into the form view of the UI, you should do so, as it is a much simpler and more
reliable way to create and edit rules.  There are some features in the string form of the rules that you can create
using the text view that are not supported in the form view.  If you choose to use some of these, by using them in the
text view, you will not be able to switch to the form view.  Examples of features you can't use in the form view are
parentheses and the Boolean "not" operator.

If you choose to use the form view, you don't need to read the rest of this section.

The Logical Expression Language is what you use to write rules. It is proprietary but very simple and straightforward.
Here is some helpful information below.

Valid elements in this language are:

* string literals (delimited by either a pair of " or ')
* boolean literals (true or false ignoring case)
* number literals (Java standard notation rules: sequence of digits 0-9, point optional for fractions)
* date literals as specified here within " or '. Valid date literals:

    `yyyy-MM-dd`

    `yyyy-MM-dd'T'HH:mm:ss` (NB: this specifies the instant associated with this time in your local time zone)

* attribute identifiers (any valid variable identifier in Java)
* relational operators (as described above)
* logical operators (and, or, not or &, |, !)


Then a condition is defined as follows:

`condition := (attribute | literal) operator (attribute | literal)`

Additional restrictions apply in that the types associated with the attribute/literal/operator in the condition have to
match.

Examples of valid conditions are:

`income > 10000`

`income > adjusted_income`

`10000 < income`

`!(age > 65) // the brackets are not necessary but improve readability`

`state = "california"`

Conditions can be chained by logical operators to create a rule, with parentheses determining precedence. Formally,
valid rules are defined as follows:

`rule := condition | (rule) | !rule | (rule || rule) | (rule && rule)`

These can be combined to an expression:

`income > 10000 & !(age > 65) | state = "california"`

Expressions are evaluated from left to right, i.e. there is an implicit left-to-right bracketing. That is, the above
expression is equivalent to:

`(income > 10000 & !(age > 65)) | state = "california"`

Parentheses can be freely inserted around binary logical operators to change the order of evaluation, creating a
different expression:

`income > 10000 & (!(age > 65) | state = "california"`