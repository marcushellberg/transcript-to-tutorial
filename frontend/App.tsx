import './App.css';
import {TextArea} from "@hilla/react-components/TextArea";
import {TextField} from "@hilla/react-components/TextField";
import {Button} from "@hilla/react-components/Button";
import 'highlight.js/styles/atom-one-light.css';
import {useState} from "react";
import {useForm} from "@hilla/react-form";
import BriefModel from "Frontend/generated/com/example/application/services/WriterService/BriefModel";
import {WriterService} from "Frontend/generated/endpoints";

export default function App() {
    const [content, setContent] = useState('');
    const [working, setWorking] = useState(false);

    const {model, field, submit} = useForm(BriefModel, {
            onSubmit: async brief => {
                setContent('');
                setWorking(true);

                WriterService.write(brief)
                    .onNext(chunk => {
                        setContent(oldContent => oldContent + chunk);
                    })
                    .onError(() => {
                        console.log("Stream failed");
                        setWorking(false);
                    })
                    .onComplete(() => {
                        setWorking(false);
                    });
            }
        }
    );

    return (
<div className="container">
    <div className="input">
        <TextArea label="Transcript" {...field(model.transcript)} />
        <TextArea label="Code" {...field(model.code)} />
        <TextField label="Keywords" {...field(model.keywords)} />
        <Button theme="primary" className="self-start" onClick={submit} disabled={working}>Generate</Button>
    </div>
    <TextArea label="Blog post" value={content} className="output"/>
</div>
    );
}
