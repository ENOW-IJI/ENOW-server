from subprocess import PIPE, Popen
import os
'''
Class : runtime
    Descriptions : 
     A class creating another thread for the source to execute
'''
class runtime:
    
    def __init__(self):
        self.resultString = "" 
    '''
    Function : run(...)
        Descriptions : 
         A function creates another thread(process) to execute the source received
        Parameters : 
            _args : A string containing a list of parameter for executing the source program
            _payloads : A string containing the payload for the source program. Usually json string.
    '''
    def run(self, _args, _payloads):
        
        fileDir = os.path.dirname(os.path.realpath('__file__'))
        sourceDir = os.path.join(fileDir, 'enow/jython/pythonSrc/preCode.py')
        
        jythonPath = "/usr/local/bin/python"
        command = []
        command.append(jythonPath)
        command.append("-u")
        command.append(sourceDir)
        
        process = Popen(args=command,
                    stdin=PIPE,
                    stdout=PIPE,
                    bufsize=1)
        
        print >> process.stdin, _payloads
        print >> process.stdin, "end\n"
        print >> process.stdin, _args
        process.stdin.close()
        for line in iter(process.stdout.readline, ''):
            self.resultString += line
            
        process.stdout.close()
        rc = process.wait()
        
        return rc
    '''
    Function : getResult()
        Descriptions : 
        A function getting the result of the run(...) function.
        Before use it, use the run(...) function first.
    '''
    def getResult(self):
        return self.resultString