grammar InputPredicate;

options {
    language = Java;
    caseInsensitive = true;
}

parse: expr=expression EOF;

expression
    : '(' expr=expression ')' # parenthesis
    | '!' expr=expression # not
    | left=expression '&' right=expression # and
    | left=expression '^' right=expression # xor
    | left=expression '|' right=expression # or
    | input=input_label # input
    ;

input_label: token=(FORWARD | BACKWARD | LEFT | RIGHT | JUMP | SNEAK | SPRINT);

WHITESPACE: [\p{White_Space}]+ -> skip;

FORWARD: 'forward';
BACKWARD: 'backward';
LEFT: 'left';
RIGHT: 'right';
JUMP: 'jump';
SNEAK: 'sneak';
SPRINT: 'sprint';
