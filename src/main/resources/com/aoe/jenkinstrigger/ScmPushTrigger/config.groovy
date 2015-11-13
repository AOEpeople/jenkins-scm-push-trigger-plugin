package com.aoe.jenkinstrigger.ScmPushTrigger

import lib.FormTagLib

f = namespace(FormTagLib)

f.entry(title: _('Use SCM trigger'), field: 'useScmTrigger') {
    f.checkbox(default: true)
}
f.entry(title: _('Custom match expression'), field: 'matchExpression') {
    f.textbox()
}
