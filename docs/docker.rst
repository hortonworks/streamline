Streamline Cluster Deployment
==============================

Dependencies
------------

1. `Docker CE <https://www.docker.com/community-edition#/download>`
2. jq (brew install jq)

Deployment
-------------


1. Build Streamline image

   ::

       ./streamline-docker.sh build

It builds the Apache Zookeeper, Apache Kafka, Schema Registry and Streamline Images. And, pulls the official images of MySQL, Oracle and Postgresql database images from the docker store.


2. Run the cluster

   ::

      ./streamline-docker.sh start

Starts Streamline, Schema Registry with all the dependent services (ZK, Kafka, Storm and DB). All the containers are connected with the private network.


Accessing Services from Host
------------------------------

Once all the containers are up, we can access Streamline, Registry and Storm UI from the host machine (OS X) by running below command to get the ports.

1. Streamline Port

   ::

       ./streamline-docker.sh port u-streamline-0

2. Schema Registry Port

   ::

       ./streamline-docker.sh port u-schema-registry

3. Storm UI Port

   ::

       ./streamline-docker.sh port u-storm-ui

Usage
======
1. Run a Single container

  ::

       ./streamline-docker.sh start ${name}

starts a single container with specified name


2. List the active containers

  ::

       ./streamline-docker.sh ps

List all the active containers that are connected with the private network


3. Lists all containers

  ::

       ./streamline-docker.sh ps-all

Lists all the containers that are connected with the private network.

4. Container Shell

  ::

       ./streamline-docker.sh shell ${name}

Login into the container and provides a Shell to the user.

5. Container Logs

  ::

        ./streamline-docker.sh logs ${name}

 Shows the logs from the container.

6. Container Exposed ports

  ::

       ./streamline-docker.sh port ${name}

Shows the exposed ports from the container to the host machine.

7. Stop the container

  ::

       ./streamline-docker.sh stop

Stops all the running containers that are connected with the private network.


8. Stop a single container

  ::

       ./streamline-docker.sh stop ${name}

9. Clean

  ::

       ./streamline-docker.sh clean

