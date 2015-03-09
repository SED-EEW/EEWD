#!/usr/bin/env python
"""
Send a test message to an AMQ broker.
Created on Mar 10, 2014

@author: behry
"""

from stompy.simple import Client
import sys, time, datetime
import xml.etree.ElementTree as ET


class AMQConnection:

    def __init__(self, host=None, port=None, topic=None, username=None,
                 password=None, continuous=False):
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
        self.continuous = continuous

    def send(self, msg):
        try:
            self.stomp.put(msg, destination=self.topic)
        except Exception, e:
                raise Exception('Cannot reconnect to server: %s' % e)

    def receive(self, f, nohb=False):
        self.stomp.subscribe(self.topic)
        if self.continuous:
            while True:
                message = self.stomp.get()
                if nohb:
                    tree = ET.fromstring(message.body)
                    if tree.tag.endswith('hb'):
                        pass
                    else:
                        print >> f, message.body
                        print >> f, "{:-^80}".format("")
                else:
                    print >> f, message.body
                    print >> f, "{:-^80}".format("")
                time.sleep(1)
            self.stomp.unsubscribe(self.topic)
            self.stomp.disconnect()
        else:
            message = self.stomp.get()
            print >> f, message.body
        self.stomp.unsubscribe(self.topic)
        self.stomp.disconnect()

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('type', help="This can be either 'receiver', 'sender' or 'heartbeat'", type=str)
    parser.add_argument("-u", "--user", help="User name.", type=str)
    parser.add_argument("-p", "--password", help="Password.", type=str)
    parser.add_argument("-H", "--host", help="Server name that is running AMQ broker.", type=str)
    parser.add_argument("-P", "--port", help="STOMP port of AMQ broker.", type=int)
    parser.add_argument("-t", "--topic", help="AMQ topic to send message to.", type=str)
    parser.add_argument("-c", help="Keep listening for messages instead of \
    closing the connection after the first received message.", action='store_true')
    parser.add_argument("-f", "--file", help="input/output file (optional)", type=str)
    parser.add_argument("-i", "--interval", help="interval (s) to send heartbeat (optional)", type=int)
    parser.add_argument("--nohb", help="When in 'receiver' mode ignore heartbeat messages.",
                        action="store_true")
    args = parser.parse_args()
    if not args.user or not args.password or not args.host or \
       not args.port or not args.topic:
        print "See the help message using the -h option."
        sys.exit(1)
    client = AMQConnection(host=args.host, port=args.port,
                           topic='/topic/' + args.topic,
                           username=args.user, password=args.password,
                           continuous=args.c)
    if args.type == 'receiver':
        if args.file:
            f = open(args.file, 'w+')
        else:
            f = sys.stdout
        client.receive(f, nohb=args.nohb)
    elif args.type == 'sender':
        data = '\nThis is a test message.\n'
        if args.file:
            with open(args.file, "r") as dataFile:
                data = dataFile.read()
        client.send(data)
    elif args.type == 'heartbeat':
        data = """<?xml version='1.0' encoding='UTF-8'?>
<hb xmlns="http://heartbeat.reakteu.org"  originator="test1" sender="test2" timestamp="%s"/>"""
        if args.file:
            with open(args.file, "r") as dataFile:
                data = dataFile.read()
        while True:
            client.send(data % datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S.%fZ'))
            if not args.interval:
                break
            time.sleep(args.interval)
    else:
        print "'type' has to be either 'receiver', 'sender' or 'heartbeat'"
