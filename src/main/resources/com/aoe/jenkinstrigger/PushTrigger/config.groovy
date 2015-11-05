package com.aoe.jenkinstrigger.PushTrigger

import lib.FormTagLib

f = namespace(FormTagLib)

f.entry(title: _('Match Expression'), field: 'matchExpression') {
    f.textbox()
}

f.entry(title: _('Use SCM Trigger'), field: 'useScmTrigger') {
    f.checkbox()
}