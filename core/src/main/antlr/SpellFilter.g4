grammar SpellFilter;

options {
    language = Java;
}

parse: expr=expression EOF;

expression
    : '(' expr=expression ')' # parenthesis
    | '!' expr=expression # not
    | left=expression '&' right=expression # and
    | left=expression '^' right=expression # xor
    | left=expression '|' right=expression # or
    | '#' tag=IDENTIFIER # tag
    | spell=IDENTIFIER # spell
    ;

WHITESPACE: [\p{White_Space}]+ -> skip;
IDENTIFIER: ~[#\p{White_Space}&|^!()] ~[\p{White_Space}&|^!()]*;
