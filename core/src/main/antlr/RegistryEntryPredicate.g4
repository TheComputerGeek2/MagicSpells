grammar RegistryEntryPredicate;

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
    | '#' tag=RESOURCE_LOCATION # tag
    | entry=RESOURCE_LOCATION # entry
    ;

WHITESPACE: [\p{White_Space}]+ -> skip;

RESOURCE_LOCATION: (NAMESPACE ':')? PATH;

NAMESPACE: [-._0-9a-z]+;

PATH: [-._/0-9a-z]+;

