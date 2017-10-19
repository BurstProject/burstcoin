#!/usr/bin/env perl
# For Emacs: -*- mode:cperl; eval: (folding-mode 1) -*-

use strict;
use warnings;

use lib 'lib';

use Test::More;
use Time::HiRes                    qw(gettimeofday
                                      sleep
                                      tv_interval);              # sub-second time taking

use BURST::API::RequestTypes qw(loop_reqtypes
                           );

my $tests = 0;

$tests += loop_reqtypes();


done_testing($tests);

