package stfXCore.Services.Frames;

import stfXCore.Models.Storyboard;
import stfXCore.Models.Thresholds.Thresholds;
import stfXCore.Services.Events.Event;
import stfXCore.Services.Events.EventDataWithTrigger;
import stfXCore.Services.Parsers.ParserFactory;
import stfXCore.Services.Events.EventWrapper;
import stfXCore.Services.StateList;
import stfXCore.Utils.Pair;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static stfXCore.Services.Events.EventWrapper.EventType.START_WRAPPER;

public class FramedDataset implements IFramedDataset {

    private Storyboard storyboard;

    private Thresholds thresholds;

    private Long initialTimestamp;

    private Long finalTimestamp;

    public FramedDataset(Storyboard storyboard, Thresholds thresholds) {
        this.storyboard = storyboard;
        this.thresholds = thresholds;

        ArrayList<Long> defaultRange = storyboard.getStates().getTemporalRange();
        this.initialTimestamp = defaultRange.get(0);
        this.finalTimestamp = defaultRange.get(1);
    }

    public FramedDataset restrictInterval(Long initialTimestamp, Long finalTimestamp) {
        if (initialTimestamp != null) this.initialTimestamp = initialTimestamp;
        if (finalTimestamp != null) this.finalTimestamp = finalTimestamp;
        return this;
    }

    public StateList getStates() {
        return this.storyboard.getStates();
    }

    public Pair<Long, Long> getInterval() {
        return new Pair(initialTimestamp, finalTimestamp);
    }

    @Override
    public ArrayList<Frame> getFrames() {
        ConcurrentLinkedQueue<Event<?>> eventsOfInterest = new ParserFactory(
                storyboard.getRigidTransformations(),
                thresholds.getParameters())
                .restrictInterval(initialTimestamp, finalTimestamp)
                .parseTransformations();

        // Priority Queue of start and end events
        PriorityQueue<EventWrapper> orderedEvents = new PriorityQueue<>(
                (e1, e2) -> {
                    long diff = e1.getTimestamp() - e2.getTimestamp();
                    return diff > 0 ? 1 : (diff == 0 ? 0 : -1);
                });
        for (Event<?> event : eventsOfInterest)
            orderedEvents.addAll(event.getWrappers());

        ArrayList<Event<?>> validEvents = new ArrayList<>();
        ArrayList<Frame> framedDataset = new ArrayList<>();
        StateList states = storyboard.getStates();

        while (!orderedEvents.isEmpty()) {
            EventWrapper eventWrapper;
            // Polling all events with equal timestamp
            do {
                eventWrapper = orderedEvents.poll();
                if (eventWrapper.getType() == START_WRAPPER)
                    validEvents.add(eventWrapper.getEvent());
                else
                    validEvents.remove(eventWrapper.getEvent());
            } while (orderedEvents.peek() != null &&
                    orderedEvents.peek().getTimestamp().equals(eventWrapper.getTimestamp()));

            if (validEvents.size() == 0)
                continue;

            Frame frame;
            if (!orderedEvents.isEmpty()) {
                frame = new Frame(states.getStates(
                        eventWrapper.getTimestamp(),
                        orderedEvents.peek().getTimestamp()));
            } else {
                frame = new Frame(states.getStates(
                        eventWrapper.getTimestamp()));
            }
            for (Event<?> validEvent : validEvents)
                frame.addEvent(new EventDataWithTrigger(
                        validEvent.getData(),
                        validEvent.getTriggerValue(frame.lowerBound(), frame.upperBound())));

            framedDataset.add(frame);
        }

        return framedDataset;
    }
}
