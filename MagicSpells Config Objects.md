MagicSpells Config Objects
===========
This is a basic guide to the configuration representation of various objects in MagicSpells. By no means is this guaranteed to be complete but rather a quick reference on how to put together common details.

Vector
--------
The vector format is `x,y,z` where each letter may be replaced with a `double` value.

TargetBooleanState
--------
The TargetBooleanState format is just a string which represents a 3 outcome type boolean state, `on`, `off`, and `toggle`.

SpellFilter
--------
The SpellFilter format is a configuration section which may contain any of the following:
- `spells` accepts a list of the internal spell names to explicitly allow.
- `denied-spells` accepts a list of the internal spell names to explicitly deny.
- `spell-tags` accepts a list of strings to indicate spell tags to look for and allow.
- `denied-spell-tags` accepts a list of strings to indicate which spell tags to look for and deny.

Spells are checked against the options in this order.
- If none of the options are defined, it allows all spells to pass through it.
- If `spells` is defined and contains the spell being checked, the spell is allowed through the filter.
- If `denied-spells` is defined and contains the spell being checked, the spell is not allowed through the filter.
- If `denied-spell-tags` is defined and the spell being checked contains a tag that is denied, the spell is not allowed through the filter.
- If `spell-tags` is defined and the spell being checked contains a tag in this collection, the spell is allowed through the filter.
- If none of these have applied, a default handling is applied. The default handling is determined as follows:
  - If `spells` or `spell-tags` are defined, the default action is to block the spell when being checked.
  - If the previous has not applied, then if `denied-spells` or `denied-spell-tags` is defined, the default action is to allow the checked spell through the filter.
  - If a default result has not been determined from the 2 above rules, the filter has no fields defined and is treated as being an open filter, meaning that it allows all spells to pass through it.

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
