#!/usr/bin/env python
"""
Send a test message to an AMQ broker.
Created on Mar 10, 2014

@author: behry
"""

from stompy.simple import Client
import sys


class AMQConnection:

    def __init__(self, host=None, port=None, topic=None, username=None,
                 password=None):
        try:
            self.topic = topic
            self.username = username
            self.password = password
            self.host = host
            self.port = port
            self.stomp = Client(host=self.host, port=self.port)
            self.stomp.connect(username=self.username, password=self.password)
        except Exception, e:
            raise Exception('Cannot connect to message broker (%s@%s:%d): %s.'\
                               % (username, host, port, e))

    def send(self, msg):
        try:
            self.stomp.put(msg, destination=self.topic)
        except Exception, e:
                raise Exception('Cannot reconnect to server: %s' % e)

    def receive(self, f):
        self.stomp.subscribe(self.topic)
        while True:
            message = self.stomp.get()
            print >> f, message.body
            #self.stomp.ack(message)

        self.stomp.unsubscribe(self.topic)
        self.stomp.disconnect()

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('type', help="This can be either 'receiver' or 'sender'", type=str)
    parser.add_argument("-u", "--user", help="User name.", type=str)
    parser.add_argument("-p", "--password", help="Password.", type=str)
    parser.add_argument("-H", "--host", help="Server name that is running AMQ broker.", type=str)
    parser.add_argument("-P", "--port", help="STOMP port of AMQ broker.", type=int)
    parser.add_argument("-t", "--topic", help="AMQ topic to send message to.", type=str)
    parser.add_argument("-f", "--file", help="input/output file (optional)", type=str)
    args = parser.parse_args()
    if not args.user or not args.password or not args.host or \
       not args.port or not args.topic:
        print "See the help message using the -h option."
        sys.exit(1)
    client = AMQConnection(host=args.host, port=args.port,
                           topic='/topic/' + args.topic,
                           username=args.user, password=args.password)

    if args.type == 'sender':
        data = '\nThis is a test message.\n'
        if args.file:
            with open(args.file, "r") as dataFile:
                data = dataFile.read()
        client.send(data)
    elif args.type == 'receiver':
        if args.file:
            f = open(args.file, 'w+')
        else:
            f = sys.stdout
        client.receive(f)
    else:
        print "'type' has to be either 'sender' or 'receiver'"
