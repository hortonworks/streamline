/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streamline.streams.runtime.processor;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.common.utils.ProcessorMsg;
import org.apache.streamline.streams.common.utils.ShellContext;
import org.apache.streamline.streams.common.utils.ShellMsg;
import org.apache.streamline.streams.common.utils.ShellProcess;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.runtime.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MultiLangProcessorRuntime implements Serializable, ProcessorRuntime {

    public static final Logger LOG = LoggerFactory.getLogger(MultiLangProcessorRuntime.class);

    public static final String COMMAND = "command";
    public static final String PROCESS_TIMEOUT_MILLS = "processTimeoutMills";
    public static final String PROCESS_CONFIG = "config";
    public static final String SHELL_CONTEXT = "context";
    public static final String OUTPUT_STREAMS = "outputStreams";
    public static final String SHELL_ENVIRONMENT = "environment";
    public static final String MULTILANG_SERIALIZER = "serializer";

    private ShellProcess shellProcess;
    private String[]  command;
    private volatile boolean running = true;
    private volatile Throwable exception;

    private int processTimeoutMills;
    private ScheduledExecutorService heartBeatExecutorService;
    private AtomicLong lastHeartbeatTimestamp = new AtomicLong();
    private AtomicBoolean waitingOnSubprocess = new AtomicBoolean(false);

    @Override
    public void initialize(Map<String, Object> config) {

        command = (String[]) config.get(COMMAND);
        processTimeoutMills = (int) config.get(PROCESS_TIMEOUT_MILLS);
        Map<String, Object> processorConfig = (Map<String, Object>) config.get(PROCESS_CONFIG);
        ShellContext shellContext = (ShellContext) config.get(SHELL_CONTEXT);
        List<String> outputStreams = (List<String>) config.get(OUTPUT_STREAMS);
        Map<String, String> envMap = (Map<String, String>) config.get(SHELL_ENVIRONMENT);
        String className = (String) config.get(MULTILANG_SERIALIZER);

        shellProcess = new ShellProcess(command);
        if(className != null)
            shellProcess.setSerializerClassName(className);
        shellProcess.setEnv(envMap);

        //subprocesses must send their pid first thing
        Long subpid = shellProcess.launch(processorConfig, shellContext, outputStreams);
        LOG.info("Launched subprocess with pid " + subpid);

        LOG.info("Start checking heartbeat...");
        setHeartbeat();

        heartBeatExecutorService = MoreExecutors.getExitingScheduledExecutorService(new ScheduledThreadPoolExecutor(1));
        heartBeatExecutorService.scheduleAtFixedRate(new HeartbeatTimerTask(this), 1, 1, TimeUnit.SECONDS);
    }


    @Override
    public List<Result> process(StreamlineEvent inputEvent) throws ProcessingException {
        if (exception != null) {
            throw new RuntimeException(exception);
        }

        return processEvent(inputEvent);
    }

    @Override
    public void cleanup() {
        heartBeatExecutorService.shutdownNow();
        shellProcess.destroy();
        running = false;
    }

    private void setHeartbeat() {
        lastHeartbeatTimestamp.set(System.currentTimeMillis());
    }

    private long getLastHeartbeat() {
        return lastHeartbeatTimestamp.get();
    }

    private void markWaitingSubprocess() {
        if(!waitingOnSubprocess.get())
            setHeartbeat();
        
        waitingOnSubprocess.compareAndSet(false, true);
    }

    private void completedWaitingSubprocess() {
        waitingOnSubprocess.compareAndSet(true, false);
    }

    private List<Result> processEvent(StreamlineEvent inputEvent) {
        List<Result> results = new LinkedList<>();

        try {
            markWaitingSubprocess();
            ProcessorMsg processorMsg = createProcessorMessage(inputEvent);
            shellProcess.writeProcessorMsg(processorMsg);

            ShellMsg errorMsg = null;
            Map<String, List<ShellMsg>> emitMsgMap =  new HashMap<>();
            while (true) {
                ShellMsg shellMsg = shellProcess.readShellMsg();
                String command = shellMsg.getCommand();
                if (command == null) {
                    throw new IllegalArgumentException("Command not found in shell message: " + shellMsg);
                }
                setHeartbeat();

                if (command.equals("sync")) {
                    break;
                } else if (command.equals("error")) {
                    errorMsg = shellMsg;
                } else if (command.equals("emit")) {
                    String stream = shellMsg.getOutputStream();
                    List<ShellMsg> eventList = emitMsgMap.get(stream);
                    if(eventList == null) {
                        eventList = new LinkedList<>();
                        emitMsgMap.put(stream, eventList);
                    }
                    eventList.add(shellMsg);
                } else {
                    throw new RuntimeException("Unknown command received: " + command);
                }
            }

            if (errorMsg != null) {
                LOG.error(errorMsg.getMsg());
                throw new ProcessingException(errorMsg.getMsg());
            }

            for (Map.Entry<String, List<ShellMsg>> entry : emitMsgMap.entrySet())
            {
                results.add(convertShellMsg(entry.getKey(), entry.getValue(), inputEvent));
            }


        } catch (Exception e) {
            String processInfo = shellProcess.getProcessInfoString() + shellProcess.getProcessTerminationInfoString();
            throw new RuntimeException(processInfo, e);
        } finally {
            completedWaitingSubprocess();
        }

        return results;
    }

    private ProcessorMsg createProcessorMessage(StreamlineEvent event) {
        ProcessorMsg processorMsg = new ProcessorMsg();
        processorMsg.setId(event.getId());
        processorMsg.setSourceId(event.getDataSourceId());
        processorMsg.setSourceStream(event.getSourceStream());
        processorMsg.setFieldsAndValues(event);
        return processorMsg;
    }

    private Result convertShellMsg(String stream,  List<ShellMsg> shellMsgList, StreamlineEvent inputEvent) {
        List<StreamlineEvent> streamlineEvents = new LinkedList<>();

        for (ShellMsg shellMsg: shellMsgList) {
            streamlineEvents.add(convertShellEvent(shellMsg.getStreamlineEvent(), inputEvent));
        }

        return new Result(stream, streamlineEvents);
    }

    private StreamlineEvent convertShellEvent(ShellMsg.ShellEvent shellEvent, StreamlineEvent inputEvent) {
        StreamlineEventImpl streamlineEvent = new StreamlineEventImpl(shellEvent.getFieldsAndValues(), inputEvent.getDataSourceId(), inputEvent.getId(), inputEvent.getHeader());
        return streamlineEvent;
    }

    private void die(Throwable exception) {
        String processInfo = shellProcess.getProcessInfoString() + shellProcess.getProcessTerminationInfoString();
        this.exception = new RuntimeException(processInfo, exception);
        String message = String.format("Halting process: Processor died. Command: %s, ProcessInfo %s",
                command,
                processInfo);
        LOG.error(message, exception);
        if (running || (exception instanceof Error)) { //don't exit if not running, unless it is an Error
            System.exit(11);
        }
    }

    private class HeartbeatTimerTask extends TimerTask {
        private MultiLangProcessorRuntime processorRuntime;

        public HeartbeatTimerTask(MultiLangProcessorRuntime processorRuntime) {
            this.processorRuntime = processorRuntime;
        }

        @Override
        public void run() {
            long lastHeartbeat = getLastHeartbeat();
            long currentTimestamp = System.currentTimeMillis();
            boolean isWaitingOnSubprocess = waitingOnSubprocess.get();

            LOG.debug("last heartbeat : {}, waiting subprocess now : {}, worker timeout (ms) : {}",
                    lastHeartbeat, isWaitingOnSubprocess, processTimeoutMills);

            if (isWaitingOnSubprocess && currentTimestamp - lastHeartbeat > processTimeoutMills) {
                processorRuntime.die(new RuntimeException("subprocess heartbeat timeout"));
            }
        }
    }

}
