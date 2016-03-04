/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Nick Sieger <nicksieger@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.io.wait;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.io.OpenFile;

import java.nio.channels.SelectionKey;

/**
 * @author Nick Sieger
 */
public class IOWaitLibrary implements Library {

    public void load(Ruby runtime, boolean wrap) {
        RubyClass ioClass = runtime.getIO();
        ioClass.defineAnnotatedMethods(IOWaitLibrary.class);
    }

    @JRubyMethod
    public static IRubyObject nread(ThreadContext context, IRubyObject _io) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        int len;
//        ioctl_arg n;
        RubyIO io = (RubyIO)_io;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(context);
        len = fptr.readPending();
        if (len > 0) return runtime.newFixnum(len);
        // TODO: better effort to get available bytes from our channel
//        if (!FIONREAD_POSSIBLE_P(fptr->fd)) return INT2FIX(0);
//        if (ioctl(fptr->fd, FIONREAD, &n)) return INT2FIX(0);
//        if (n > 0) return ioctl_arg2num(n);
        // Because we can't get an actual system-level buffer available count, we fake it by returning 1 if ready
        return RubyNumeric.int2fix(runtime, fptr.readyNow(context) ? 1 : 0);
    }

    /**
     * returns non-nil if input available without blocking, false if EOF or not open/readable, otherwise nil.
     */
    @JRubyMethod(name = "ready?")
    public static IRubyObject ready(ThreadContext context, IRubyObject _io) {
        RubyIO io = (RubyIO)_io;
        Ruby runtime = context.runtime;
        OpenFile fptr;
//        ioctl_arg n;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(context);
        if (fptr.readPending() != 0) return runtime.getTrue();
        // TODO: better effort to get available bytes from our channel
//        if (!FIONREAD_POSSIBLE_P(fptr->fd)) return Qnil;
//        if (ioctl(fptr->fd, FIONREAD, &n)) return Qnil;
//        if (n > 0) return Qtrue;
        return runtime.newBoolean(fptr.readyNow(context));
    }

    @JRubyMethod(name = {"wait", "wait_readable"}, optional = 1)
    public static IRubyObject wait_readable(ThreadContext context, IRubyObject _io, IRubyObject[] argv) {
        RubyIO io = (RubyIO)_io;
        Ruby runtime = context.runtime;
        OpenFile fptr;
        boolean i;
//        ioctl_arg n;
        IRubyObject timeout;
        long tv;

        fptr = io.getOpenFileChecked();
        fptr.checkReadable(context);

        switch (argv.length) {
            case 1:
                timeout = argv[0];
                break;
            default:
                timeout = context.nil;
        }

        if (timeout.isNil()) {
            tv = -1;
        }
        else {
            tv = (long)(RubyTime.convertTimeInterval(context, timeout) * 1000);
            if (tv < 0) throw runtime.newArgumentError("time interval must be positive");
        }

        if (fptr.readPending() != 0) return runtime.getTrue();
        boolean ready = fptr.ready(runtime, context.getThread(), SelectionKey.OP_READ | SelectionKey.OP_ACCEPT, tv);
        fptr.checkClosed();
        if (ready) return io;
        return context.nil;
    }

    /**
     * waits until input available or timed out and returns self, or nil when EOF reached.
     */
    @JRubyMethod(optional = 1)
    public static IRubyObject wait_writable(ThreadContext context, IRubyObject _io, IRubyObject[] argv) {
        RubyIO io = (RubyIO)_io;
        Ruby runtime = context.runtime;
        OpenFile fptr;
        IRubyObject timeout;
        long tv;

        fptr = io.getOpenFileChecked();
        fptr.checkWritable(context);

        switch (argv.length) {
            case 1:
                timeout = argv[0];
                break;
            default:
                timeout = context.nil;
        }
        if (timeout.isNil()) {
            tv = -1;
        }
        else {
            tv = (long)(RubyTime.convertTimeInterval(context, timeout) * 1000);
            if (tv < 0) throw runtime.newArgumentError("time interval must be positive");
        }

        boolean ready = fptr.ready(runtime, context.getThread(), SelectionKey.OP_WRITE, tv);
        fptr.checkClosed();
        if (ready)
            return io;
        return context.nil;
    }
}
