/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.RubyNode;

public class UnlessNode extends RubyNode {

    @Child private BooleanCastNode condition;
    @Child private RubyNode thenBody;

    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public UnlessNode(RubyContext context, SourceSection sourceSection, RubyNode condition, RubyNode thenBody) {
        super(context, sourceSection);

        this.condition = BooleanCastNodeGen.create(context, sourceSection, condition);
        this.thenBody = thenBody;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (!conditionProfile.profile(condition.executeBoolean(frame))) {
            return thenBody.execute(frame);
        } else {
            return nil();
        }
    }

}
