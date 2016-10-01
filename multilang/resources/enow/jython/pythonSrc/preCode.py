import sys
import thread
import json
import codecs
import logging
import os
from time import sleep
sys.path.append(r'/Users/jeasungpark/Downloads/Eclipse.app/Contents/Eclipse/plugins/org.python.pydev_5.1.2.201606231256/pysrc')

fileDir = os.path.dirname(os.path.realpath('__file__'))
modulePath = os.path.join(fileDir, 'enow/jython/pythonSrc')
sys.path.append(modulePath)

from body import eventHandler
from postCode import postProcess
from StreamToLogger import StreamToLogger
import pydevd
'''
List : Global Variables
    Descriptions :
        threadExit : A variable for detecting whether a thread executing the body source has exited"
        loggerStdout : A variable logging the stream passed out on STDOUT
        loggerStderr : A variable logging the stream passed out on STDERR
        lock = A semaphore assigned from thread
'''
threadExit = False
lock = 0

def kilobytes(megabytes):
    return megabytes * 1024 * 1024

def eventHandlerFacade(_event, _context, _callback):
    global threadExit
    global lock

    CURRENT_DIR = os.path.abspath(os.path.dirname(__file__))
    loggerStdoutFilePath = os.path.join(CURRENT_DIR, 'log', 'log.txt')

    logging.basicConfig(
                       level=logging.DEBUG,
                       format='%(asctime)s:%(levelname)s:%(name)s:%(message)s',
                       filename=loggerStdoutFilePath,
                       filemode='a'
                       )

    stdout_logger = logging.getLogger('STDOUT')
    sl = StreamToLogger(stdout_logger, logging.INFO)
    sys.stdout = sl

    stderr_logger = logging.getLogger('STDERR')
    sl = StreamToLogger(stderr_logger, logging.ERROR)
    sys.stderr = sl

    eventHandler(_event, _context, _callback)


def Main():

    jsonDump = ""
    parameterDump = ""
    previousDataDump = ""
    _event = None
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    while True:
        binaryString = sys.stdin.readline()
        
        if not binaryString:
            break

        if binaryString == b"endl\n":
            break

        jsonDump += codecs.encode(binaryString, 'utf-8')

    while True:
        binaryString = sys.stdin.readline()
        
        if not binaryString:
            break
        
        if binaryString == b"endl\n":
            break

        parameterDump += codecs.encode(binaryString, 'utf-8')

    while True:
        binaryString = sys.stdin.readline()
        
        if not binaryString:
            break
        
        if binaryString == b"endl\n":
            break

        previousDataDump += codecs.encode(binaryString, 'utf-8')

    if jsonDump != "null":
        _event = json.loads(jsonDump)
    _context = dict()
    _callback = dict()
    _previousData = json.loads(previousDataDump)
    
    
    
    """
    context object written in json
    ATTRIBUTES:
        * function_name
        * function_version
        * invoked_ERN
        * memory_limit_in_mb
    """
    _context["function_name"] = ""
    _context["function_version"] = ""
    _context["invoked_ERN"] = ""
    _context["memory_limit_in_mb"] = 64
    _context["topicName"] = ""
    _context["deviceID"] = ""
    _context["parameter"] = parameterDump
    _context["previousData"] = _previousData

    """
    setting up a thread for executing a body code
    """
    global lock
    lock = thread.allocate_lock()

    stackSize = []
    stackSize.append(kilobytes(_context["memory_limit_in_mb"]))
    thread.stack_size(kilobytes(64))

    """
    setting up a logger for debugging
    """
    try:
        thread.start_new_thread(eventHandlerFacade, (_event, _context, _callback))
    except:
        sys.stderr.write(str("Error\n"))
    
    sleep(1)

    sys.stdout = old_stdout
    sys.stderr = old_stderr
    postProcess(_event, _context, _callback)

if __name__ == "__main__":
    '''
    sys.stderr.write("preCode.py : running")
    sys.stderr.flush()
    '''
    Main()
    '''
    sys.stderr.write("preCode.py : exiting")
    sys.stderr.flush()
    '''
