sequenceDiagram
    autonumber
    Note over Sender,Counterparty: << Soroban >>

    Counterparty->>+Sender: Step 1: inputs, premixOutputs, changeOutput
    Sender->>+Counterparty: Step 2: inputs, premixOutputs, changeOutput, counterpartyChangeOutput-=(minerFee/2)
    Counterparty->>+Sender: Step 3: sign
    Sender->>+Counterparty: Step 4: sign
    