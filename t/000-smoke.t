#!/usr/bin/env perl
# For Emacs: -*- mode:cperl; eval: (folding-mode 1) -*-

use strict;
use warnings;

use lib 'lib';

use Test::More;

use Data::Dumper;
use Data::Rx;
use LWP::UserAgent;
use LWP::Protocol::https;
use JSON                           qw(-support_by_pp);           # need JSON to support bignum
use Math::BigInt;
use Time::HiRes                    qw(gettimeofday
                                      sleep
                                      tv_interval);              # sub-second time taking

use BURST::API::RequestTypes qw($reqtypes);

my $tests = 0;

my $ua      = LWP::UserAgent->new(
#    ssl_opts => { verify_hostname => 1 },
);
my $URLBASE = 'http://localhost:8125/burst?requestType=';

my %config = (
    server_url     => $URLBASE,
    max_retries => 3,
);

my $rx = Data::Rx->new;

LOOP_REQS:
for my $rtype (@{$reqtypes}) {
    next LOOP_REQS if ($rtype->{skip});

    my $rt_name   = $rtype->{name};
    my $rt_schema = build_schema($rtype);
    my $rt_args   = $rtype->{args};
    my $reply     = talk2wallet($rt_name, $rt_args);
    #print Dumper($reply);

    my $valid = $rt_schema->check($reply);
    ok($valid, $rt_name);
    $tests++;

    if (!$valid) {
        print Dumper($reply);
        my $rx_failure = $rt_schema->assert_valid($reply);
        print Dumper($rx_failure);
    }
}


#my $request  = HTTP::Request->new(GET => $config{ssl_dl_url} . $path);


done_testing($tests);


sub build_url {
    my $reqtype = shift;
    
    return 'http://localhost:8125/burst?requestType=' . $reqtype;
}


# {{{ talk2wallet                  talk to the wallet

sub talk2wallet {
    my $path    = shift;                # relative URL on server
    my $send    = shift;                # data to send to server
    my $verbose = shift // 1;           # do we want status reports (default: yes)

    my $json    = JSON->new->utf8->allow_blessed->allow_bignum;       # create JSON object with features
    my $request;

    if (defined $send) {
#    print "$config{server_url}$path\n";

    #     my $content = $json->encode($send);                               # convert Perl structure to JSON
    #     my $header  = HTTP::Headers->new(                                 # construct HTTP header
    #         Content_Length => length($content),
    #         Content_Type   => 'application/json;charset=utf-8'
    #     );
    #     $request = HTTP::Request->new('POST',
    #                                   "$config{server_url}$path",
    #                                   $header,
    #                                   $content);
    # }
    # else {
        $path .= ('&' . hash2get($send));
    }


    
    $request = HTTP::Request->new('GET',
                                  "$config{server_url}$path");

    my $retries  = $config{max_retries};                    # number of retries in case the server does not answer
    my $response = _get_srv_response($request, $verbose);   # hold the response from the server.

    return $json->decode($response);                        # decode answer to Perl structure
}

# }}}
# {{{ _get_srv_response            get response from server given request and retries

sub _get_srv_response {
    my $request = shift;
    my $verbose = shift // 1;

    my $retries = $config{max_retries};      # number of retries in case the server does not answer
    my $response;                            # hold the response from the server.

  SRVCON_LOOP:
    while ($retries--) {                                         # prepare to have to retry connecting to server
        $response = $ua->request($request);                      # get answer

        last SRVCON_LOOP if ($response->is_success);        # end connect loop if all ok

        # HERE: Problems connecting to server. So retry.
        my $status = $response->status_line;                     # get status line message
        _out_unbuffered("\nProblem connecting to server " . $request->uri . "(status: $status). Retries left: $retries\n", $verbose);
        cleanup_end($status) if (!$retries);                     # end program with status line if out of retries
        my $sleep = sprintf("%2.3f", 5 * ($config{max_retries} - $retries) + rand(20));  # get sleep length
        _out_unbuffered("Sleeping ${sleep} s...\n", $verbose);   # inform user about sleep
        sleep $sleep;                                            # sleep, then retry
    }

    return $response->content;
}

# }}}

# {{{ cleanup & end                cleanup & end / graceful termination

sub cleanup_end {
    my $msg = shift;

    if (defined $msg) {
        _out_unbuffered("$msg\n");
    }
    ReadMode('normal') if ($^O ne 'MSWin32');
#    $pm->finish()      if (defined $pm && $cpus > 1);

    exit 0;
}

# }}}
# {{{ _out_unbuffered              unbuffered output

sub _out_unbuffered {
    my $str  = shift;         # message to show
    my $show = shift // 1;    # do we actually want it to be shown (default: yes)

    $| = 1;                   # unbuffered STDOUT (prints do not wait for newline)
    print $str if($show);     # do the print if show is on
    $| = 0;                   # restore to buffered STDOUT

    return;
}

# }}}



sub build_schema {
    my $rtype = shift;
    my $schema = {
        type     => '//rec',
        required => $rtype->{required},
    };

    #print Dumper($schema);
    return $rx->make_schema($schema);
}



sub hash2get {
    my $href = shift;

    my $str = join '&', map { "$_=$$href{$_}" } (keys %{$href});

    #print "STR: $str\n";

    return $str;
}
