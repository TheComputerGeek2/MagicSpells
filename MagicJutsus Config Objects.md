MagicJutsus Config Objects
===========
This is a basic guide to the configuration representation of various objects in MagicJutsus. By no means is this guaranteed to be complete but rather a quick reference on how to put together common details.

Vector
--------
The vector format is `x,y,z` where each letter may be replaced with a `double` value.

TargetBooleanState
--------
The TargetBooleanState format is just a string which represents a 3 outcome type boolean state, `on`, `off`, and `toggle`.

JutsuFilter
--------
The JutsuFilter format is a configuration section which may contain any of the following:
- `jutsus` accepts a list of the internal jutsu names to explicitly allow.
- `denied-jutsus` accepts a list of the internal jutsu names to explicitly deny.
- `jutsu-tags` accepts a list of strings to indicate jutsu tags to look for and allow.
- `denied-jutsu-tags` accepts a list of strings to indicate which jutsu tags to look for and deny.

Jutsus are checked against the options in this order.
- If none of the options are defined, it allows all jutsus to pass through it.
- If `jutsus` is defined and contains the jutsu being checked, the jutsu is allowed through the filter.
- If `denied-jutsus` is defined and contains the jutsu being checked, the jutsu is not allowed through the filter.
- If `denied-jutsu-tags` is defined and the jutsu being checked contains a tag that is denied, the jutsu is not allowed through the filter.
- If `jutsu-tags` is defined and the jutsu being checked contains a tag in this collection, the jutsu is allowed through the filter.
- If none of these have applied, a default handling is applied. The default handling is determined as follows:
  - If `jutsus` or `jutsu-tags` are defined, the default action is to block the jutsu when being checked.
  - If the previous has not applied, then if `denied-jutsus` or `denied-jutsu-tags` is defined, the default action is to allow the checked jutsu through the filter.
  - If a default result has not been determined from the 2 above rules, the filter has no fields defined and is treated as being an open filter, meaning that it allows all jutsus to pass through it.

Prompt
--------
- `prompt-type` accepts a `string` and will fail if not set. Current valid values are
  - `regex`
  - `fixed-set`
  - `enum`

In addition to any additional options specified by the format of the specific prompt type.

RegexPrompt
--------
- Everything that MagicPromptResponder has
- `regexp` accepts a `string` and fails if not set to a valid regular expression.
- `prompt-text` accepts a `string` to use as the prompt's message to the player.

FixedSetPrompt
--------
- Everything that MagicPromptResponder has
- `options` accepts a `list` of strings and fails if not set.
- `prompt-text` accepts a `string` to use as the prompt's message to the player.

EnumSetPrompt
--------
- Everything that MagicPromptResponder has
- `enum-class` accepts a `string` of the class to load the value options from.
- `prompt-text` accepts a `string` to use as the prompt's message to the player.

MagicPromptResponder
--------
- `variable-name` accepts a `string` of the variable name to save the validated prompt response to.

ConversationFactory
--------
- `prefix` accepts a `string` and defaults to nothing.
- `local-echo` accepts a `boolean` and defaults to `true`.
- `first-prompt` accepts a `configuration section` in `prompt` format.
- `timeout-seconds` accepts an `integer` and defaults to `30`.
- `escape-sequence` accepts a `string` and defaults to nothing.
