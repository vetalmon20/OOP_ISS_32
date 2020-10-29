import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class MySkipList {
    class Node {
        final int value;
        final int towerLevel;
        final AtomicMarkableReference<Node>[] next;

        boolean markedForRemoval;

        @SuppressWarnings("unchecked")
        public Node(int value, int towerLevel, int maxLevel) {
            this.value = value;
            this.towerLevel = towerLevel;
            this.next = (AtomicMarkableReference<Node>[])
                    new AtomicMarkableReference<?>[maxLevel];
            this.markedForRemoval = false;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    final AtomicMarkableReference<Node> header;
    final AtomicInteger currentListLevel;
    final AtomicInteger size;
    final int maxLevel;

    MySkipList(int maxLevel){
        this.currentListLevel = new AtomicInteger(0);
        this.size = new AtomicInteger(0);
        this.maxLevel = maxLevel;
        Node headerNode = new Node(Integer.MAX_VALUE, 0, maxLevel);
        header = new AtomicMarkableReference<>(headerNode, true);
        for (int level = 0; level < maxLevel; level++) {
            headerNode.next[level] = header;
        }
    }

    public int size() { return size.get();}

    public boolean contains (int inputValue) {
        AtomicMarkableReference<Node> currentListElement = this.header;
        AtomicMarkableReference<Node> previousListElement = this.header;
        Node currentNode = currentListElement.getReference();

        int level = currentListLevel.get() - 1;

        for (; level >= 0; level--) {

            currentListElement = currentNode.next[level];
            currentNode = currentListElement.getReference();
            
            while (currentNode.value < inputValue) {
                if(level == currentListLevel.get() - 1 && currentListElement == this.header){
                    return false;
                }
                previousListElement = currentListElement;
                currentListElement = currentNode.next[level];
                currentNode = currentListElement.getReference();
            }

            currentListElement = previousListElement;
            currentNode = currentListElement.getReference();
        }

        currentListElement = currentNode.next[0];
        currentNode = currentListElement.getReference();

        return currentNode.value == inputValue;
    }

    public boolean add(Integer inputValue){
        AtomicMarkableReference<Node> currentListElement = this.header;
        AtomicMarkableReference<Node> previousListElement = this.header;
        Node currentNode = currentListElement.getReference();

        Node[] update = new Node[maxLevel];
        int levels = currentListLevel.get();
        for(int level = levels - 1; level >= 0; level--) {
            currentListElement = currentNode.next[level];
            currentNode = currentListElement.getReference();

            while (currentNode.value < inputValue) {
                previousListElement = currentListElement;
                currentListElement = currentNode.next[level];
                currentNode = currentListElement.getReference();
            }
            update[level] = previousListElement.getReference();

            currentListElement = previousListElement;
            currentNode = currentListElement.getReference();
        }

        currentListElement = currentNode.next[0];
        currentNode = currentListElement.getReference();

        if(currentNode.value == inputValue) {
            return false;
        } else {
            int newLevel = Utils.getRandomLevel(this.maxLevel);

            //if the level is higher, we create new level that equals currLevel + 1
            if (newLevel >= levels) {
                currentListLevel.incrementAndGet();
                levels += 1;
                newLevel = levels - 1;
                update[newLevel] = header.getReference();
                //header.attemptMark(header.getReference(), false);
            }

            Node newNode = new Node(inputValue, newLevel, maxLevel);
            AtomicMarkableReference<Node> atomicNewNode =
                    new AtomicMarkableReference<>(newNode, true);

            for (int level = 0; level < levels; level++) {
                newNode.next[level] = update[level].next[level];
                //Node beforeNode = update[level].next[level].getReference();
                //update[level].next[level].attemptMark(beforeNode, true);
                update[level].next[level] = atomicNewNode;
            }

            size.incrementAndGet();

            return true;
        }


    }

    public boolean remove(Integer inputValue){
        AtomicMarkableReference<Node> currentListElement = this.header;
        AtomicMarkableReference<Node> previousListElement = this.header;
        Node currentNode = currentListElement.getReference();

        Node[] update = new Node[maxLevel];
        int levels = currentListLevel.get();
        for(int level = levels - 1; level >= 0; level--) {
            currentListElement = currentNode.next[level];
            currentNode = currentListElement.getReference();

            while (currentNode.value < inputValue) {
                previousListElement = currentListElement;
                currentListElement = currentNode.next[level];
                currentNode = currentListElement.getReference();
            }
            update[level] = previousListElement.getReference();

            currentListElement = previousListElement;
            currentNode = currentListElement.getReference();
        }

        currentListElement = currentNode.next[0];
        currentNode = currentListElement.getReference();

        if (currentNode.value == inputValue) {
            currentNode.markedForRemoval = true;
            for (int level = 0; level < levels; level++) {
                update[level].next[level] = currentNode.next[level];
                if (update[level].next[level] == null) {
                    update[level].next[level] = header;
                }
            }

            size.decrementAndGet();

            //delete excess levels of the tower
            int oldLevels = levels;
            while (levels > 1 && header.getReference().next[levels] == header) {
                levels = currentListLevel.decrementAndGet();
            }

            for (int level = 0; level < oldLevels; level++) {
                AtomicMarkableReference<Node> link = update[level].next[level];
                Node temp;
                do {
                    temp = link.getReference();
                } while (! link.attemptMark(temp, true));
            }

            return true;
        }

        return false;
    }


    @Override
    public String toString() {
        AtomicMarkableReference<Node> current = header;
        Node currentNode = current.getReference();
        current = currentNode.next[0];
        currentNode = current.getReference();

        StringBuilder out = new StringBuilder();
        out.append("[");
        while(current != header){
            out.append(currentNode.value);
            current = currentNode.next[0];

            currentNode = current.getReference();
            if(current != header){
                out.append(" ");
            }
        }
        out.append("]");

        return out.toString();
    }
}
