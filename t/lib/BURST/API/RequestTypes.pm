# For Emacs: -*- mode:cperl; eval: (folding-mode 1); coding:utf-8; -*-
package BURST::API::RequestTypes;

# {{{ use block

use warnings;
use strict;
use utf8;
use feature ':5.12';

use Data::Dumper;
use Data::Rx;
use Data::Rx::Type::PCRE;

use JSON                           qw(-support_by_pp);           # need JSON to support bignum
use LWP::UserAgent;
use LWP::Protocol::https;

use Test::More;

# }}}
# {{{ var block

our @ISA = qw(Exporter);
our @EXPORT = qw(loop_reqtypes
            );

my $ua      = LWP::UserAgent->new(
#    ssl_opts => { verify_hostname => 1 },
);


my %config = (
    protocol    => ($ENV{BRS_APITEST_PROTO}  // 'http'),
    server      => ($ENV{BRS_APITEST_SERVER} // 'localhost'),
    port        => ($ENV{BRS_APITEST_PORT}   // 8125),
    max_retries => 3,
);

my $API_URL = $config{protocol}
            . '://'
            . $config{server}
            . ':'
            . $config{port}
            . '/burst'
            ;

# }}}
# {{{ Data::Rx definitions


my $CGPX = 'tag:burst.cryptoguru.org,2018:rx'; # own CG prefix

my $rx = Data::Rx->new({
    type_plugins => [ 'Data::Rx::Type::PCRE' ]
});

my $ph_number = $rx->make_schema({
    type  => 'tag:rjbs.manxome.org,2008-10-04:rx/pcre/str',
    regex => q/\A867-[5309]{4}\z/,
});

$rx->add_prefix(
    brs => "$CGPX/"
);

$rx->learn_type(
    "$CGPX/balanceBURST" => {
        type  => '//int',
        range => {
            min => 0,
            max => 2_158_812_800,
        },
    }
);

$rx->learn_type(
    "$CGPX/balancePLANCK" => {
        type  => '//int',
        range => {
            min => 0,
            max => 215_881_280_000_000_000,
        },
    }
);


# skip: default 0 (if to skip that test)
# meth: http method (default GET)
# args: default none/undef
our $reqtypes = [ # we want to define a sequence of tests
    # broadcastTransaction
    # buyAlias
    # {{{ calculateFullHash
    {
        name => 'calculateFullHash',
        required => {
            fullHash => {
                type  => '//str',
                value => 'f151ded1194ce627ff44bd67ab25cdddb9a35ff2006653c0f9d25a2de5ad463e',
            },
            requestProcessingTime => '//int',
        },
        args => {
            unsignedTransactionBytes => '010203',
            signatureHash            => '010101',
        },
    },
    # }}}
    # cancelAskOrder
    # cancelBidOrder
    # createATProgram
    # decryptFrom
    # dgsDelisting
    # dgsDelivery
    # dgsFeedback
    # dgsListing
    # dgsPriceChange
    # dgsPurchase
    # dgsQuantityChange
    # dgsRefund
    # encryptTo
    # escrowSign
    # generateToken
    # getAT
    # getATDetails
    # {{{ getATIds
    {
        name => 'getATIds',
        required => {
            atIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },

    },
    # }}}
    # {{{ getAccount
     {
         name => 'getAccount',
         txt  => 'get account with UTF-8 user name',
         required => {
             accountRS => {
                 type => '//str',
                 value => 'BURST-DPQM-9X88-LEU3-CNSST',
             },
             account => {
                 type => '//int',
                 value => 12457823256334161619,
             },
             assetBalances => {
                 type => '//arr',
                 contents => '//any',
             },
             unconfirmedAssetBalances => {
                 type => '//arr',
                 contents => '//any',
             },
             balanceNQT            => '/brs/balancePLANCK',
             effectiveBalanceNXT   => '/brs/balancePLANCK',
             forgedBalanceNQT      => '/brs/balancePLANCK',
             guaranteedBalanceNQT  => '/brs/balancePLANCK',
             unconfirmedBalanceNQT => '/brs/balancePLANCK',
             name => '//str',
             publicKey => '//str',

             requestProcessingTime => '//int',
         },
         args => {
             account => 'BURST-DPQM-9X88-LEU3-CNSST'
         },
     },
    # }}}

    # getAccountATs
    # getAccountBlockIds
    # getAccountBlocks
    # getAccountCurrentAskOrderIds
    # getAccountCurrentAskOrders
    # getAccountCurrentBidOrderIds
    # getAccountCurrentBidOrders
    # getAccountEscrowTransactions
    # getAccountId
    # getAccountLessors
    # getAccountPublicKey
    # getAccountSubscriptions
    # getAccountTransactionIds
    # getAccountTransactions
    # getAccountsWithRewardRecipient
    # getAlias
    # getAliases

    # {{{ getAllAssets
    {
        name => 'getAllAssets',
        meth => 'POST',
        required => {
            assets => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenAskOrders
    {
        name => 'getAllOpenAskOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenBidOrders
    {
        name => 'getAllOpenBidOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}

    # getAllTrades
    # getAskOrder
    # getAskOrderIds
    # getAskOrders
    # getAsset
    # getAssetAccounts

    # {{{ getAssetIds
    {
        name => 'getAssetIds',
        required => {
            assetIds => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}

    # getAssetTransfers
    # getAssets
    # getAssetsByIssuer
    # getBalance
    # getBidOrder
    # getBidOrderIds
    # getBidOrders
    # {{{ getBlock
    {
        name => 'getBlock',
        required => {
            previousBlockHash => '//str',
            payloadLength => '//int',
            totalAmountNQT => '//int',
            generationSignature => '//str',
            generator => '//int',
            generatorPublicKey => '//str',
            baseTarget => '//int',
            payloadHash => '//str',
            generatorRS => '//str',
            blockReward => '//int',
            scoopNum => '//int',
            numberOfTransactions => '//int',
            blockSignature => '//str',
            transactions => {
                type => '//arr',
                contents => '//int',
            },
            nonce => '//int',
            version => '//int',
            totalFeeNQT => '//int',
            previousBlock => '//int',
            block => '//int',
            height => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # getBlockId

    # {{{ getBlockchainStatus
    {
        name => 'getBlockchainStatus',
        required => {
            lastBlock => '//int',
            application => {
                type => '//str',
                value => "BRS",
            },
            time       => '//int',
            version    => '//str',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfBlocks => '//int',
            lastBlockchainFeeder => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getBlocks
    {
        name => 'getBlocks',
        required => {
            blocks => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getConstants
    {
        skip => 1,
        name => 'getConstants',
        required => {
            maxBlockPayloadLength => {
                type => '//int',
                value => 44880,
            },
            maxArbitraryMessageLength => {
                type => '//int',
                value => 1000,
            },
            genesisBlockId => {
                type => '//int',
                value => 3444294670862540038,
            },
            "transactionTypes" => '//arr',
        },
    },
    # }}}
    # {{{ getDGSGoods
    {
        name => 'getDGSGoods',
        required => {
            goods => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # getDGSPendingPurchases
    # getDGSPurchase
    # {{{ getDGSPurchases
    {
        name => 'getDGSPurchases',
        required => {
            purchases => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getECBlock
    {
        name => 'getECBlock',
        required => {
            ecBlockHeight => '//int',
            ecBlockId => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # getEscrowTransaction
    # getGuaranteedBalance
    # {{{ getMiningInfo
    {
        name => 'getMiningInfo',
        required => {
            baseTarget   => '//int',
            height => '//int',
            generationSignature => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getMyInfo
    {
        name => 'getMyInfo',
        required => {
            address => '//str',
            host    => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getPeers
    {
        name => 'getPeers',
        required => {
            peers => {
                type     => '//arr',
                contents => '//str',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # getRewardRecipient
    # {{{ getState

    {
        name => 'getState',
        required => {
            numberOfPeers => '//int',
            numberOfUnlockedAccounts => '//int',
            numberOfTransfers => '//int',
            numberOfOrders => '//int',
            numberOfTransactions => '//int',
            maxMemory => '//int',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            numberOfAssets => '//int',
            freeMemory => '//int',
            availableProcessors => '//int',
            totalEffectiveBalanceNXT => '/brs/balanceBURST',
            numberOfAccounts => '//int',
            numberOfBlocks => '//int',
            version => '//str',
            numberOfBidOrders => '//int',
            lastBlock => '//int',
            totalMemory => '//int',
            application => '//str',
            numberOfAliases => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfTrades => '//int',
            time => '//int',
            numberOfAskOrders => '//int',
            lastBlockchainFeeder => '//str',
            requestProcessingTime => '//int',
        },
    },

    # }}}
    # getSubscription
    # getSubscriptionsToAccount
    # {{{ getTime
    {
        name => 'getTime',
        txt  => 'get wallet time in seconds since genesis block',
        required => {
            time => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # getTrades
    # getTransaction
    # getTransactionBytes
    # {{{ getUnconfirmedTransactionIds
    {
        name => 'getUnconfirmedTransactionIds',
        debug => 1,
        required => {
            unconfirmedTransactionIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getUnconfirmedTransactions
    {
        name     => 'getUnconfirmedTransactions',
        required => {
            unconfirmedTransactions => {
                type => '//arr',
                contents => '//any'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # issueAsset
    # {{{ longConvert
    {
        name => 'longConvert',
        required => {
            stringId => {
                type => '//int',
                value => 100,
            },
            longId => {
                type => '//int',
                value => 100,
            },
            requestProcessingTime => '//int',
        },
        args => {
            id => 100,
        },
    },
    # }}}
    # parseTransaction
    # placeAskOrder
    # placeBidOrder
    # readMessage
    # {{{ rsConvert
    {
        name => 'rsConvert',
        txt  => 'convert numeric ID to RS address',
        required => {
            accountRS => {
                type => '//str',
                value => "BURST-EMXC-3PFB-J8HQ-GJ9HZ",
            },
            account => {
                    type => '//int',
                    value => 17274946210831421354,
                },
            requestProcessingTime => '//int',
        },
        args => {
            account => 17274946210831421354,
        },
    },
    # }}}

    # sellAlias
    # sendMessage
    # sendMoney
    # sendMoneyEscrow
    # sendMoneyMulti
    # sendMoneyMultiSame
    # sendMoneySubscription
    # setAccountInfo
    # setAlias
    # setRewardRecipient
    # signTransaction
    # submitNonce
    # subscriptionCancel

];

# }}}

# {{{ loop_reqtypes

sub loop_reqtypes {
    my $tests = 0;

  LOOP_REQS:
    for my $rtype (@{$reqtypes}) {                          # iterate all tests
        my $rt_name = $rtype->{name};                       # get reqestType names (defines also name of test)

        if ($rtype->{skip}) {                               # Skip handling
            say "$rt_name skipped by user request.";
            next LOOP_REQS;
        }

        my $rt_args   = {                                   # build hashref with arguments for a given requestType
            requestType => $rt_name,                        # the requestType itself is a key => value argument
            %{$rtype->{args} // {}}                               # add the argument payload
        };

        my $rt_meth = $rtype->{meth} // 'GET';              # use HTTP method (default: GET)
        my $reply   = talk2wallet($rt_meth, $rt_args);      # make the API request

        print Dumper($reply) if $rtype->{debug};            # debug what's going on

        my $rt_schema = build_schema($rtype);               # build the Data::Rx schema for the validation
        my $valid     = $rt_schema->check($reply);          # perform the Data::Rx schema check
        my $testtxt   = $rt_name                            # build test text
                      . ($rtype->{txt} ? " - $$rtype{txt}"  # with more than just req name
                                       : '')                # if available
                                       ;
        ok($valid, $testtxt);                               # judge the actual test (valid/not valid) and inform user
        $tests++;

        if (!$valid) {                                      # if test result wasn't valid/no success
            eval {                                          # catch exceptions
                say Dumper($reply);
                my $rx_failure  = $rt_schema->assert_valid($reply);
                my @rx_failures = $rx_failure->failures;
            };
            if ($@) {
                say $@;
            }
        }
    }

    return $tests;
}

# }}}

# {{{ build_schema

sub build_schema {
    my $rtype = shift;
    my $schema = {
        type     => '//rec',
        required => $rtype->{required},
    };

    #print Dumper($schema);
    return $rx->make_schema($schema);
}

# }}}

# {{{ talk2wallet                  talk to the wallet

sub talk2wallet {
    my $method  = shift;                # use GET/POST/...
    my $data    = shift;                # data to send to server
    my $verbose = shift // 1;           # do we want status reports (default: yes)

    my $URI = $API_URL . '?' . hash2get($data);

    #say "$method -> $URI";

    my $request = HTTP::Request->new($method,
                                     $URI);

    my $retries  = $config{max_retries};                              # number of retries in case the server does not answer
    my $response = _get_srv_response($request, $verbose);             # hold the response from the server.
    my $json     = JSON->new->utf8->allow_blessed->allow_bignum;      # create JSON object with features (for wallet answer)

    return $json->decode($response);                                  # decode JSON answer to Perl structure
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

        last SRVCON_LOOP if ($response->is_success);             # end connect loop if all ok

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

# {{{ hash2get

sub hash2get {
    my $href = shift;

    my $str = join '&', map { "$_=$$href{$_}" } (keys %{$href});

    #print "STR: $str\n";

    return $str;
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


return 1;
