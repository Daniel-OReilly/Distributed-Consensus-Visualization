import drawSvg as draw
import datetime
import math
import time
import json

class Event:
    def __init__(self, physical_time, vector_clock, message):
        self.physical_time = physical_time
        self.vector_clock = vector_clock
        self.message = message

class Node:
    def __init__(self, node_name):
        self.node_name = node_name
        self.events = []

    def add_event(self, physical_time, vector_clock, message):
        self.events.append(Event(physical_time, vector_clock, message))

class MSDGenerator:

    def __init__(self, file_name):

        #Load in the log records, converting them into data structures.
        #Calculate metadata about them which is used to help generate the graphic.
        min_physical_time, max_physical_time, num_events, nodes = load_file(file_name)
        #Convert the min and max times into number of seconds.
        min_time_seconds = time.mktime(min_physical_time.timetuple())
        max_time_seconds = time.mktime(max_physical_time.timetuple())

        node_separation_distance = 100
        node_top_distance = -100
        avg_event_separation_distance = 50
        node_left_distance = 150
        image_width = 150 + (node_separation_distance * len(nodes))

        #Calculate the height of the image by looking at the number of events we have and assume they are evenly
        #distributed. Add a top and bottom margin as well.
        image_height = -(2 * node_top_distance) + (avg_event_separation_distance * num_events)

        #Start a new SVG drawing with the given width and height.
        #Not really sure what origin does. It involves something about where the coordinate origin is, but not 100%.
        d = draw.Drawing(image_width, image_height, origin=(0, -image_height), displayInline=False)

        #-----------------------------#
        # Add the top dots and node names.
        node_offset = node_left_distance
        for node in nodes:
            d.append(draw.Circle(node_offset, node_top_distance, 10, fill='blue'))
            d.append(draw.Text(node.node_name, 12, node_offset, -80, center=0.1, fill='black'))
            node_offset += node_separation_distance

        #-----------------------------#
        #Add a vertical line for each node representing the events occurring on it.
        node_offset = node_left_distance
        for node in nodes:
            d.append(draw.Line(node_offset, node_top_distance - 10, node_offset, -(image_height - 30),  stroke='black', stroke_width=2,))
            node_offset += node_separation_distance

        #-----------------------------#
        #Add physical time annotation.
        num_annotations = math.floor(num_events / 2)
        annotation_height = node_top_distance - avg_event_separation_distance
        time_offset = (max_time_seconds - min_time_seconds) / num_annotations

        #Record timeline start and end y coordinates so we can position events relative to the timeline.
        timeline_start_y = annotation_height

        for i in range(num_annotations + 1):
            formatted_time = datetime.datetime.fromtimestamp(min_time_seconds + (i * time_offset)).strftime("%H:%M:%S")
            d.append(draw.Line(node_left_distance - 50,  annotation_height, image_width - (node_separation_distance / 2), annotation_height, stroke='black', stroke_width=1, stroke_dasharray="5.5"))
            d.append(draw.Text(formatted_time, 12, node_left_distance - 80,  annotation_height - 2, center=0.1, fill='black'))
            annotation_height -= 2 * avg_event_separation_distance

        #The last part of the addition is to account for the fact that annotation_height was incremented
        #one addition time before the loop completed.
        timeline_end_y = annotation_height + (2 * avg_event_separation_distance)

        #-----------------------------#
        #Add a circle for each event.
        #Calculate which events require connections.

        #Stores the locations of previously rendered events.
        #This info allows us to draw lines connecting related events.
        #Each entry is of the format (node_name, local_vector_clock) -> (X,Y) coords
        event_coord_map = {}

        node_offset = node_left_distance
        for node in nodes:
            current_event_distance = node_top_distance - avg_event_separation_distance
            for event in node.events:
                event_time_seconds = time.mktime(event.physical_time.timetuple())
                event_time_percentage = (event_time_seconds - min_time_seconds) / (max_time_seconds - min_time_seconds)
                event_y_coord = timeline_start_y + ((timeline_end_y - timeline_start_y) * event_time_percentage)
                event_coord_map[(node.node_name, event.vector_clock[node.node_name])] = (node_offset, event_y_coord)
                d.append(draw.Circle(node_offset, event_y_coord, 5, fill='black'))
                current_event_distance -= avg_event_separation_distance
            node_offset += node_separation_distance

        #-----------------------------#
        #Add a diagonal line for each pair of events.

        #Iterate over each node.
        for node in nodes:

            #For the given node, we will keep track of the vector count as we iterate over its local events.
            #This allows us to determine if a clock from non-local count changes. If it does, then we have to
            #add a line as a message was received.
            current_vector_clock = {}
            for event in node.events:

                #Get the current vector count for the given event.
                current_vector_count = event.vector_clock[node.node_name]

                #Iterate over all count values for the events vector clock.
                for key, value in event.vector_clock.items():
                    #We only care about the values of non-local clocks since those are the ones that require
                    #lines to be added to the graphic.
                    if key != node.node_name:

                        #We only add a line if the given clock is either added to the current clock or its value changes
                        #This signifies that a message has been received from the given node.
                        if key not in current_vector_clock or value > current_vector_clock[key]:
                            #Update the clocks value.
                            current_vector_clock[key] = value
                            #Perform a lookup to see where the two events were drawn.
                            current_event_coords = event_coord_map[(node.node_name, current_vector_count)]
                            corresponding_event_coords = event_coord_map[(key, value)]
                            #Add a line connecting the two events.
                            d.append(draw.Line(current_event_coords[0], current_event_coords[1], corresponding_event_coords[0],
                                               corresponding_event_coords[1], stroke='black', stroke_width=2, ))



        #-----------------------------#
        #Render SVG image to file.
        d.setRenderSize(image_width, image_height)
        d.saveSvg('images/example.svg')


def load_file(file_name):
    nodes = {}
    min_physical_time = None
    max_physical_time = None
    num_events = 0

    with open(file_name, 'r') as reader:
        while True:
            # Each event in the log consists of two consecutive lines.
            # The first line contains metadata info about the event.
            # The second line contains the message associated with the event.
            metadata = reader.readline()
            message = reader.readline().strip()

            # If either line is None, then we are at the end of the log. Stop processing lines.
            if not metadata or not message:
                break

            # Break up the metadata line into the given components.
            node_name, physical_time, vector_clock = metadata.split(" ", 2)
            physical_time = datetime.datetime.strptime(physical_time, "%H:%M:%S")
            vector_clock = json.loads(vector_clock)

            # Check if this event has the smallest time.
            if min_physical_time is None:
                min_physical_time = physical_time
            else:
                min_physical_time = min(physical_time, min_physical_time)

            # Check if this event has the largest time.
            if max_physical_time is None:
                max_physical_time = physical_time
            else:
                max_physical_time = max(physical_time, max_physical_time)

            # Increase the number of events counter.
            num_events += 1

            # If this is the first message for this node in the logs, create a new Node object for it.
            if node_name not in nodes:
                nodes[node_name] = Node(node_name)

            # Add the event to the given node.
            nodes[node_name].add_event(physical_time, vector_clock, message)

    return min_physical_time, max_physical_time, num_events, [node for _, node in nodes.items()]


file_name = ""
MSDGenerator(file_name)