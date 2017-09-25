.. highlight:: kotlin
.. raw:: html

   <script type="text/javascript" src="_static/jquery.js"></script>
   <script type="text/javascript" src="_static/codesets.js"></script>

Running our CorDapp
===================

Now that we've written a CorDapp, it's time to test it by running it on some real Corda nodes.

Clean up
--------
Before running our node, delete the ``client/TemplateClient.java`` (for Java) or ``client/TemplateClient.kt`` (for
Kotlin) file. We won't be using it, and it will cause build errors unless we remove it.

Deploying our CorDapp
---------------------
Let's take a look at the nodes we're going to deploy. Open the project's ``build.gradle`` file and scroll down to the
``task deployNodes`` section. This section defines three nodes - the Controller, NodeA, and NodeB:

.. container:: codeset

    .. code-block:: kotlin

        task deployNodes(type: net.corda.plugins.Cordform, dependsOn: ['jar']) {
            directory "./build/nodes"
            networkMap "O=Controller,OU=corda,L=London,C=UK"
            node {
                name "O=Controller,OU=corda,L=London,C=UK"
                advertisedServices = ["corda.notary.validating"]
                p2pPort 10002
                rpcPort 10003
                cordapps = []
            }
            node {
                name "CN=NodeA,O=NodeA,L=London,C=UK"
                advertisedServices = []
                p2pPort 10005
                rpcPort 10006
                webPort 10007
                cordapps = []
                rpcUsers = [[ user: "user1", "password": "test", "permissions": []]]
            }
            node {
                name "CN=NodeB,O=NodeB,L=New York,C=US"
                advertisedServices = []
                p2pPort 10008
                rpcPort 10009
                webPort 10010
                cordapps = []
                rpcUsers = [[ user: "user1", "password": "test", "permissions": []]]
            }
        }

We have three standard nodes, plus a special Controller node that is running the network map service, and is also
advertising a validating notary service. Feel free to add additional node definitions here to expand the size of the
test network.

We can run this ``deployNodes`` task using Gradle. For each node definition, Gradle will:

* Package the project's source files into a CorDapp jar
* Create a new node in ``build/nodes`` with our CorDapp already installed

We can do that now by running the following commands from the root of the project:

.. code:: python

    // On Windows
    gradlew clean deployNodes

    // On Mac
    ./gradlew clean deployNodes

Running the nodes
-----------------
Running ``deployNodes`` will build the nodes under ``build/nodes``. If we navigate to one of these folders, we'll see
the three node folders. Each node folder has the following structure:

    .. code:: python

        .
        |____corda.jar                     // The runnable node
        |____corda-webserver.jar           // The node's webserver
        |____dependencies
        |____node.conf                     // The node's configuration file
        |____cordapps
          |____java/kotlin-source-0.1.jar  // Our IOU CorDapp

Let's start the nodes by running the following commands from the root of the project:

.. code:: python

    // On Windows
    build/nodes/runnodes.bat

    // On Mac
    build/nodes/runnodes

This will start a terminal window for each node, and an additional terminal window for each node's webserver - eight
terminal windows in all. Give each node a moment to start - you'll know it's ready when its terminal windows displays
the message, "Welcome to the Corda interactive shell.".

  .. image:: resources/running_node.png
     :scale: 25%
     :align: center

Interacting with the nodes
--------------------------
Now that our nodes are running, let's order one of them to create an IOU by kicking off our ``IOUFlow``. In a larger
app, we'd generally provide a web API sitting on top of our node. Here, for simplicity, we'll be interacting with the
node via its built-in CRaSH shell.

Go to the terminal window displaying the CRaSH shell of Node A. Typing ``help`` will display a list of the available
commands.

We want to create an IOU of 100 with Node B. We start the ``IOUFlow`` by typing:

.. container:: codeset

    .. code-block:: java

        start IOUFlow arg0: 99, arg1: "NodeB"

    .. code-block:: kotlin

        start IOUFlow iouValue: 99, otherParty: "NodeB"

Node A and Node B will automatically agree an IOU. If the flow worked, it should have led to the recording of a new IOU
in the vaults of both Node A and Node B.

We can check the flow has worked by using an RPC operation to check the contents of each node's vault. Typing ``run``
will display a list of the available commands. We can examine the contents of a node's vault by running:

.. code:: python

     run vaultAndUpdates

And we can also examine a node's transaction storage, by running:

.. code:: python

     run verifiedTransactions

The vaults of Node A and Node B should both display the following output:

.. code:: python

    first:
    - state:
        data:
          value: 99
          lender: "CN=NodeA,O=NodeA,L=London,C=GB"
          borrower: "CN=NodeB,O=NodeB,L=New York,C=US"
          contract: {}
          participants:
          - "CN=NodeA,O=NodeA,L=London,C=GB"
          - "CN=NodeB,O=NodeB,L=New York,C=US"
        notary: "O=Controller,OU=corda,L=London,C=GB,OU=corda.notary.validating"
        encumbrance: null
      ref:
        txhash: "656A1BF64D5AEEC6F6C944E287F34EF133336F5FC2C5BFB9A0BFAE25E826125F"
        index: 0
    second: "(observable)"

Conclusion
----------
We have written a simple CorDapp that allows IOUs to be issued onto the ledger. Like all CorDapps, our
CorDapp is made up of three key parts:

* The ``IOUState``, representing IOUs on the ledger
* The ``IOUContract``, controlling the evolution of IOUs over time
* The ``IOUFlow``, orchestrating the process of agreeing the creation of an IOU on-ledger

Together, these three parts completely determine how IOUs are created and evolved on the ledger.

Next steps
----------
There are a number of improvements we could make to this CorDapp:

* We could require signatures from the lender as well the borrower, to give both parties a say in the creation of a new
  ``IOUState``
* We should add unit tests, using the contract-test and flow-test frameworks
* We should change ``IOUState.value`` from an integer to a proper amount of a given currency
* We could add an API, to make it easier to interact with the CorDapp

We will explore some of these improvements in future tutorials. But you should now be ready to develop your own
CorDapps. There's `a more fleshed-out version of the IOU CorDapp <https://github.com/corda/cordapp-tutorial>`_ with an
API and web front-end, and a set of example CorDapps in `the main Corda repo <https://github.com/corda/corda>`_, under
``samples``. An explanation of how to run these samples :doc:`here <running-the-demos>`.

As you write CorDapps, you can learn more about the API available :doc:`here <api>`.

If you get stuck at any point, please reach out on `Slack <https://slack.corda.net/>`_,
`Discourse <https://discourse.corda.net/>`_, or `Stack Overflow <https://stackoverflow.com/questions/tagged/corda>`_.
